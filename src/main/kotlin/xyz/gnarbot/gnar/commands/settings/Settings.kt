package xyz.gnarbot.gnar.commands.settings

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.parsers.MemberParser
import me.devoxin.flight.internal.parsers.RoleParser
import me.devoxin.flight.internal.parsers.TextChannelParser
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import xyz.gnarbot.gnar.Bot
import xyz.gnarbot.gnar.commands.template.annotations.Description
import xyz.gnarbot.gnar.utils.extensions.DEFAULT_SUBCOMMAND
import xyz.gnarbot.gnar.utils.extensions.data
import xyz.gnarbot.gnar.utils.extensions.isGuildPremium
import xyz.gnarbot.gnar.utils.extensions.premiumGuild
import xyz.gnarbot.gnar.utils.toDuration
import java.lang.RuntimeException
import java.time.Duration
import kotlin.reflect.KFunction

class Settings : Cog {
    @Command(aliases = ["setting", "set", "config", "configuration", "configure", "opts", "options"],
        description = "Change music settings.", guildOnly = true, userPermissions = [Permission.MANAGE_SERVER])
    fun settings(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @SubCommand(description = "Resets the settings for the guild.")
    fun reset(ctx: Context) {
        ctx.data.apply { reset(); save() }
        ctx.send {
            setColor(0x9570D3)
            setTitle("Settings")
            setDescription("The settings for this server have been reset.")
        }
    }

    @SubCommand(aliases = ["autodel"], description = "Toggle whether the bot auto-deletes its responses.")
    fun autodelete(ctx: Context, toggle: Boolean) {
        val data = ctx.data

        data.command.isAutoDelete = toggle
        data.save()

        val send = if (!toggle) "The bot will no longer automatically delete messages after 10 seconds."
        else "The bot will now delete messages after 10 seconds."

        ctx.send(send)
    }

    @SubCommand(aliases = ["ta"])
    fun announcements(ctx: Context, toggle: Boolean) {
        val data = ctx.data
        data.music.announce = toggle
        data.save()

        val send = if (toggle) "Announcements for music enabled." else "Announcements for music disabled."
        ctx.send(send)
    }

    @SubCommand
    fun djonly(ctx: Context, toggle: Boolean) {
        val data = ctx.data
        data.command.isDjOnlyMode = toggle
        data.save()

        val send = if (toggle) "Enabled DJ-only mode." else "Disabled DJ-only mode."
        ctx.send(send)
    }

    @SubCommand(aliases = ["djrequirement"], description = "Set whether DJ-only commands can be used by all.")
    fun requiredj(ctx: Context, toggle: Boolean) {
        val data = ctx.data
        data.music.isDisableDj = !toggle
        data.save()

        val send = if (toggle) "DJ commands now require the DJ role." else "DJ commands can be now run by everyone."
        ctx.send(send)
    }

    @SubCommand(aliases = ["vc"], description = "Toggles a voice-channel as a dedicated music channel.")
    fun voicechannel(ctx: Context, channel: VoiceChannel) {
        val data = ctx.data

        if (channel.id in data.music.channels) {
            data.music.channels.remove(channel.id)
            data.save()
            return ctx.send("${channel.name} is no longer a designated music channel.")
        }

        if (channel == ctx.guild!!.afkChannel) {
            return ctx.send("`${channel.name}` is the AFK channel, you can't play music there.")
        }

        data.music.channels.add(channel.id)
        data.save()
        ctx.send("`${channel.name}` is now a designated music channel.")
    }

    @SubCommand(aliases = ["sl"], description = "Set the maximum song length. \"reset\" to reset.")
    fun songlength(ctx: Context, content: String) {
        val data = ctx.data

        if (content == "reset") {
            data.music.maxSongLength = 0
            data.save()
            return ctx.send("Song length limit reset.")
        }

        val duration = try {
            content.toDuration()
        } catch (e: RuntimeException) {
            return ctx.send("Wrong duration specified: Expected something like `40 minutes`")
        }

        val config = Bot.getInstance().configuration
        val premiumGuild = ctx.premiumGuild
        val durationLimit = premiumGuild?.songLengthQuota ?: config.durationLimit.toMillis()

        if (duration.toMillis() > durationLimit) {
            return ctx.send("This is too much. The limit is ${config.durationLimitText}.")
        }

        if (duration.toMinutes() < 1) {
            return ctx.send("That's too little. It has to be more than 1 minute.")
        }

        data.music.maxSongLength = duration.toMillis()
        data.save()
        ctx.send("Successfully set song length limit to $content.")
    }

    @SubCommand(aliases = ["ac"], description = "Set the music announcement channel. Omit to reset.")
    fun announcementchannel(ctx: Context, textChannel: TextChannel?) {
        ctx.data.let {
            it.music.announcementChannel = textChannel?.id
            it.save()
        }

        val out = textChannel?.let { "Successfully set music announcement channel to ${it.asMention}" }
            ?: "Successfully reset the music announcement channel."

        ctx.send(out)
    }
}
