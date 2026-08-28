package dev.chasem.cobblemonextras.lang

import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.chasem.cobblemonextras.CobblemonExtras
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Every message this mod sends, resolved on the server.
 *
 * Deliberately not Component.translatable: this runs on servers whose players
 * use a vanilla client, and a translation key is resolved by the CLIENT. Anyone
 * without the mod would read "cobblemonextras.pokeshout.header" instead of a
 * sentence. Resolving here means every player sees the same text, in the
 * language the server chose, whatever client they run.
 *
 * Each message carries its own colours as section codes, and the shared chat
 * prefix as the token <prefix>. Nothing about how a message looks is decided in
 * code: an operator restyles the whole mod from the files, and the prefix is
 * written once, under 'prefix'.
 *
 * A file in lang/ is an OVERLAY, not a replacement: the bundled file is read
 * first and the config file laid on top of it, key by key. A server writes only
 * what it changes and still picks up every message a later version adds.
 *
 * That is also why the defaults are NOT copied into lang/ itself. A full copy
 * sitting there would shadow the bundled file forever, and an update's new
 * wording would never reach a server that had booted once. They go to
 * lang/defaults/, rewritten every boot, purely to be read and copied from.
 *
 * Lookup order for a key:
 *   1. the configured language: bundled, then overlaid from lang/
 *   2. en_us: bundled, then overlaid from lang/
 *   3. the key itself, so a missing entry is visible instead of blank
 */
object ExtrasLang {

    /** Shipped with the jar and always present, which is what makes it the fallback. */
    const val DEFAULT_LANGUAGE = "en_us"

    /** Languages shipped in the jar, and dumped to lang/defaults/ for reference. */
    private val BUNDLED = listOf("en_us", "es_es")

    /** Holds the chat prefix, so a rebrand is one line rather than one per message. */
    private const val PREFIX_KEY = "prefix"

    /** What a message writes where the prefix should go. */
    private const val PREFIX_TOKEN = "<prefix>"

    private val GSON = Gson()
    private val strings = HashMap<String, String>()
    private val fallback = HashMap<String, String>()

    /**
     * @param language the value of the 'language' config option
     */
    fun load(language: String?) {
        strings.clear()
        fallback.clear()

        // A config written before this option existed has no 'language' key and
        // the platform hands back null. Treat that as "use the default" rather
        // than looking for a file called null.json.
        val chosen = if (language.isNullOrBlank()) DEFAULT_LANGUAGE else language

        val langDir = File(
            System.getProperty("user.dir") + File.separator + "config" +
                File.separator + CobblemonExtras.MODID + File.separator + "lang"
        )
        writeReferenceDefaults(File(langDir, "defaults"))

        // The fallback is read first and separately: if the chosen language is
        // missing a key, that key still has to resolve to something readable.
        readBundledInto(fallback, DEFAULT_LANGUAGE)
        readInto(fallback, File(langDir, "$DEFAULT_LANGUAGE.json"))

        if (DEFAULT_LANGUAGE == chosen) {
            strings.putAll(fallback)
        } else {
            readBundledInto(strings, chosen)
            readInto(strings, File(langDir, "$chosen.json"))
            if (strings.isEmpty()) {
                CobblemonExtras.getLogger().warn(
                    "No language file for '$chosen', bundled or in $langDir, falling back to $DEFAULT_LANGUAGE"
                )
                strings.putAll(fallback)
            }
        }
        resolvePrefix()
        CobblemonExtras.getLogger().info("Loaded ${strings.size} messages for language '$chosen'")
    }

    /** A message, formatted and ready to send. Section signs in the file are honoured. */
    fun get(key: String, vararg args: Any): MutableComponent = Component.literal(raw(key, *args))

    /** The formatted text, for the few places that need a String. */
    fun raw(key: String, vararg args: Any): String {
        val pattern = strings[key] ?: fallback[key] ?: return key
        if (args.isEmpty()) {
            return pattern
        }
        return try {
            String.format(pattern, *args)
        } catch (e: RuntimeException) {
            // A file edited by hand can easily disagree with the code about how
            // many arguments a message takes. One broken line must not take the
            // command down with it.
            CobblemonExtras.getLogger().warn("Bad format for '$key': ${e.message}")
            pattern
        }
    }

    /**
     * A key read as a STYLE rather than as text.
     *
     * Some pieces are components the server must not flatten — a species name,
     * an item name, a nature — because they are translatable and each client
     * should read them in its own language. Their text cannot come from here,
     * but their colour can: the key holds only formatting codes and this turns
     * them into a Style to hang on the component.
     */
    fun style(key: String): Style {
        var style = Style.EMPTY
        val codes = strings[key] ?: fallback[key] ?: return style
        var i = 0
        while (i < codes.length - 1) {
            if (codes[i] == '§') {
                when (val formatting = ChatFormatting.getByCode(codes[i + 1])) {
                    null -> Unit
                    ChatFormatting.RESET -> style = Style.EMPTY
                    else -> style = style.applyFormat(formatting)
                }
                i += 2
            } else {
                i++
            }
        }
        return style
    }

    /**
     * Expands <prefix> in every message, in both maps.
     *
     * Done once at load rather than per message: this runs on every chat line
     * the mod sends. A language that does not set a prefix of its own borrows
     * the fallback's, so a one-key override file styles the whole mod.
     */
    private fun resolvePrefix() {
        val prefix = strings[PREFIX_KEY] ?: fallback[PREFIX_KEY] ?: ""
        expandPrefix(strings, prefix)
        expandPrefix(fallback, prefix)
    }

    private fun expandPrefix(target: MutableMap<String, String>, prefix: String) {
        for (entry in target.entries) {
            if (entry.value.contains(PREFIX_TOKEN)) {
                entry.setValue(entry.value.replace(PREFIX_TOKEN, prefix))
            }
        }
    }

    /**
     * Dumps the shipped languages somewhere an operator can read them.
     *
     * Overwritten every boot on purpose: these are a printout of what the jar
     * currently contains, not configuration. Editing one changes nothing — the
     * file to edit is lang/<language>.json, one directory up, and it only needs
     * the keys being changed.
     */
    private fun writeReferenceDefaults(defaultsDir: File) {
        if (!defaultsDir.exists() && !defaultsDir.mkdirs()) {
            CobblemonExtras.getLogger().error("Could not create $defaultsDir")
            return
        }
        for (language in BUNDLED) {
            val target = File(defaultsDir, "$language.json")
            try {
                open(language)?.use { bundled -> target.outputStream().use { bundled.copyTo(it) } }
            } catch (e: Exception) {
                CobblemonExtras.getLogger().error("Could not write $target", e)
            }
        }
    }

    private fun readInto(target: MutableMap<String, String>, file: File) {
        if (!file.exists()) {
            return
        }
        try {
            file.reader(StandardCharsets.UTF_8).use { parseInto(target, it.readText()) }
        } catch (e: Exception) {
            CobblemonExtras.getLogger().error("Could not read $file", e)
        }
    }

    private fun readBundledInto(target: MutableMap<String, String>, language: String) {
        try {
            open(language)?.use { parseInto(target, InputStreamReader(it, StandardCharsets.UTF_8).readText()) }
        } catch (e: Exception) {
            CobblemonExtras.getLogger().error("Could not read bundled language $language", e)
        }
    }

    private fun parseInto(target: MutableMap<String, String>, json: String) {
        val root = JsonParser.parseString(json).asJsonObject
        for ((key, value) in root.entrySet()) {
            if (key.startsWith("_")) {
                continue
            }
            target[key] = value.asString
        }
    }

    private fun open(language: String) =
        ExtrasLang::class.java.getResourceAsStream("/assets/${CobblemonExtras.MODID}/lang/$language.json")
}
