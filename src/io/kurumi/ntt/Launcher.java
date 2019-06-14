package io.kurumi.ntt;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RuntimeUtil;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import io.kurumi.ntt.db.BotDB;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.BotServer;
import io.kurumi.ntt.fragment.Donate;
import io.kurumi.ntt.fragment.abs.Callback;
import io.kurumi.ntt.fragment.abs.Msg;
import io.kurumi.ntt.fragment.abs.request.Send;
import io.kurumi.ntt.fragment.admin.Alias;
import io.kurumi.ntt.fragment.admin.Control;
import io.kurumi.ntt.fragment.admin.DelMsg;
import io.kurumi.ntt.fragment.admin.Firewall;
import io.kurumi.ntt.fragment.admin.Notice;
import io.kurumi.ntt.fragment.admin.Users;
import io.kurumi.ntt.fragment.base.Final;
import io.kurumi.ntt.fragment.base.GetIDs;
import io.kurumi.ntt.fragment.base.PingFunction;
import io.kurumi.ntt.fragment.bots.MyBots;
import io.kurumi.ntt.fragment.bots.NewBot;
import io.kurumi.ntt.fragment.bots.UserBot;
import io.kurumi.ntt.fragment.debug.Backup;
import io.kurumi.ntt.fragment.debug.DebugMsg;
import io.kurumi.ntt.fragment.debug.DebugStatus;
import io.kurumi.ntt.fragment.debug.DebugUser;
import io.kurumi.ntt.fragment.forum.admin.ForumManage;
import io.kurumi.ntt.fragment.group.AntiEsu;
import io.kurumi.ntt.fragment.group.AutoReply;
import io.kurumi.ntt.fragment.group.BanSetickerSet;
import io.kurumi.ntt.fragment.group.ChineseAction;
import io.kurumi.ntt.fragment.group.GroupRepeat;
import io.kurumi.ntt.fragment.twitter.action.Block;
import io.kurumi.ntt.fragment.twitter.action.Follow;
import io.kurumi.ntt.fragment.twitter.action.Jvbao;
import io.kurumi.ntt.fragment.twitter.action.Mute;
import io.kurumi.ntt.fragment.twitter.action.UnBlock;
import io.kurumi.ntt.fragment.twitter.action.UnFollow;
import io.kurumi.ntt.fragment.twitter.action.UnMute;
import io.kurumi.ntt.fragment.twitter.auto.AutoUI;
import io.kurumi.ntt.fragment.twitter.delete.TwitterDelete;
import io.kurumi.ntt.fragment.twitter.ext.BlockList;
import io.kurumi.ntt.fragment.twitter.ext.StatusGetter;
import io.kurumi.ntt.fragment.twitter.login.ShadowBan;
import io.kurumi.ntt.fragment.twitter.login.TwitterLogin;
import io.kurumi.ntt.fragment.twitter.login.TwitterLogout;
import io.kurumi.ntt.fragment.twitter.status.StatusAction;
import io.kurumi.ntt.fragment.twitter.status.StatusFetch;
import io.kurumi.ntt.fragment.twitter.status.StatusSearch;
import io.kurumi.ntt.fragment.twitter.status.StatusUpdate;
import io.kurumi.ntt.fragment.twitter.timeline.TimelineUI;
import io.kurumi.ntt.fragment.twitter.track.TrackTask;
import io.kurumi.ntt.fragment.twitter.track.TrackUI;
import io.kurumi.ntt.utils.BotLog;

import java.io.IOException;
import java.util.TimeZone;

public class Launcher extends BotFragment implements Thread.UncaughtExceptionHandler {

    public static final Launcher INSTANCE = new Launcher();

    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));

        if (!INSTANCE.isLongPulling()) {

            int serverPort = Integer.parseInt(Env.getOrDefault("server_port", "-1"));
            String serverDomain = Env.get("server_domain");

            while (serverPort == -1) {

                System.out.print("输入本地Http服务器端口 : ");

                try {

                    serverPort = Integer.parseInt(Console.input());

                    Env.set("server_port", serverPort);

                } catch (Exception e) {
                }

            }

            if (serverDomain == null) {

                System.out.print("输入BotWebHook域名 : ");

                serverDomain = Console.input();

                Env.set("server_domain", serverDomain);

            }

            BotServer.INSTANCE = new BotServer(serverPort, serverDomain);

            try {

                BotServer.INSTANCE.start();

            } catch (IOException e) {

                BotLog.error("端口被占用 请检查其他BOT进程。");

                return;

            }

        }

        String dbAddr = Env.getOrDefault("db_address", "127.0.0.1");
        Integer dbPort = Integer.parseInt(Env.getOrDefault("db_port", "27017"));

        while (!initDB(dbAddr, dbPort)) {

            System.out.print("输入MongoDb地址 : ");
            dbAddr = Console.scanner().nextLine();

            try {

                System.out.print("输入MongoDb端口 : ");
                dbPort = Console.scanner().nextInt();

                Env.set("db_address", dbAddr);
                Env.set("db_port", dbPort);

            } catch (Exception e) {
            }

        }

        RuntimeUtil.addShutdownHook(new Runnable() {

            @Override
            public void run() {

                INSTANCE.stop();

            }

        });

        BotLog.info("正在启动...");

        INSTANCE.start();

    }

    static boolean initDB(String dbAddr, Integer dbPort) {

        try {

            BotDB.init(dbAddr, dbPort);

            return true;

        } catch (Exception e) {

            return false;

        }

    }

    @Override
    public boolean onMsg(UserData user, Msg msg) {

        if (super.onMsg(user, msg)) return true;

        if ("start".equals(msg.command()) && msg.params().length == 0) {

            msg.send("start failed successfully ~", "", "NTT是一只开源TelegramBot、可以作为Twitter客户端使用、也可以导出贴纸、创建私聊BOT、以及在群内沙雕发言与复读。", "", "BOT帮助文档请戳 : @NTT_X", "交流群组在这里 : @NTTDiscuss").html().publicFailed();

            return true;

        } else if ("help".equals(msg.command())) {

            msg.send("文档在 @NTT_X ~").publicFailed();

            return true;

        }

        return false;

    }

    @Override
    public void start() {

        super.start();

        TimelineUI.start();

        // AutoTask.start();

        TrackTask.start();

        UserBot.startAll();

        Backup.start();

    }


    // public MtProtoBot mtp;

    @Override
    public boolean silentStart() {

        if (super.silentStart()) {

            // AutoTask.start();

            TimelineUI.start();

            TrackTask.start();

            UserBot.startAll();

            Backup.start();

            return true;

        }

        return false;

    }

    @Override
    public void realStart() {

        // Base Functions

        addFragment(new Firewall());

        addFragment(new PingFunction());

        addFragment(new GetIDs());

        addFragment(new Notice());

        addFragment(new DelMsg());

        addFragment(new Alias());

        addFragment(new Backup());

        addFragment(new Users());

        addFragment(new DebugMsg());

        addFragment(new Control());

        // 贴吧

        // addFragment(new TiebaLogin());

        // Twitter Action

        addFragment(new Follow());

        addFragment(new UnFollow());

        addFragment(new Mute());

        addFragment(new UnMute());

        addFragment(new Block());

        addFragment(new UnBlock());

        // Twitter

        addFragment(new Jvbao());

        addFragment(new DebugUser());

        addFragment(new DebugStatus());

        addFragment(new StatusUpdate());

        addFragment(new ShadowBan());

        addFragment(new StatusSearch());

        addFragment(new StatusGetter());

        addFragment(new StatusFetch());

        addFragment(new TwitterLogin());

        addFragment(new TwitterLogout());

        addFragment(new AutoUI());

        addFragment(new TrackUI());

        addFragment(new StatusAction());
        addFragment(new TimelineUI());

        addFragment(new BlockList());

        addFragment(new GroupRepeat());

        addFragment(new ChineseAction());

        addFragment(new AntiEsu());

        addFragment(new BanSetickerSet());

        addFragment(new AutoReply());

        addFragment(new TwitterDelete());

        // Forum

        addFragment(new ForumManage());

        // Bots

        addFragment(new NewBot());
        addFragment(new MyBots());

        // Donate

        addFragment(new Donate());

        addFragment(new Final());

        super.realStart();

		/*

		 new Timer().schedule(new TimerTask() {

		 @Override
		 public void run() {

		 // if no task run , process will exit

		 }

		 },10 * 60 * 1000);

		 */

    }

    @Override
    public void stop() {

        // AutoTask.stop();

        // mtp.stopBot();

        for (BotFragment bot : BotServer.fragments.values()) {

            if (bot != this) bot.stop();

        }

        super.stop();

        BotServer.INSTANCE.stop();

        /*

         FTTask.stop();
         UTTask.stop();
         SubTask.stopAll();

         */

        TimelineUI.stop();

        TrackTask.stop();

        Backup.stop();

        //  BotServer.INSTACNCE.stop();

    }

    @Override
    public String botName() {

        return "NTTBot";

    }

    @Override
    public boolean isLongPulling() {

        return false;

    }

    @Override
    public boolean onUpdate(UserData user, Update update) {

        BotLog.process(user, update);

        if (update.message() != null) {

            if (update.message().chat().type() == Chat.Type.Private && (user.contactable == null || !user.contactable)) {

                user.contactable = true;

                UserData.userDataIndex.put(user.id, user);

                UserData.data.setById(user.id, user);

            }

        }

        return false;

    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        BotLog.error("无法处理的错误,正在停止BOT", throwable);

        new Send(Env.GROUP, "NTT 异常退出", BotLog.parseError(throwable)).exec();

        INSTANCE.stop();

        System.exit(1);

    }

    @Override
    public boolean onCallback(UserData user, Callback callback) {

        if (callback.params.length == 0 || (callback.params.length == 1 && "null".equals(callback.params[0]))) {

            callback.confirm();

            return true;

        }

        return false;

    }

}
