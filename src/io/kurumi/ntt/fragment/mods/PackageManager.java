package io.kurumi.ntt.fragment.mods;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.BotLog;

import java.io.File;
import java.util.List;

public class PackageManager extends Fragment {

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerAdminFunction("yum");

    }

    final String GIT_FORCE_UPDATE = "git fetch --depth=1 origin master && git checkout -f FETCH_HEAD && git clean -fdx";

    File envPath = new File(Env.DATA_DIR, "mods/env");

    String executeGitCommand(File path, String... command) {

        path.mkdirs();

        return RuntimeUtil.getResult(RuntimeUtil.exec(null, path, command));

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        String[] mainHelp = new String[]{

                "用法  : /yum <命令> [参数...]",

                "\n/yum install <package>",
                // "/yum show <package>",
                // "/yum reinstall <package>",
                // "/yum uninstall <package>",
                // "/yum search <query>",
                // "/yum update",
                // "/yum list-local",
                // "/yum list-all",

        };

        if (params.length == 0) {

            msg.send(ArrayUtil.join(mainHelp, "\n")).async();

            return;

        }

        Msg status = msg.send("正在更新模块源...").send();

        if (!new File(envPath, ".git").isDirectory()) {

            RuntimeUtil.execForStr("git clone " + Env.MODULES_REPO + "/packages " + envPath.getPath());

        } else {

            executeGitCommand(envPath, "git pull");

            // executeGitCommand("git fetch --depth=1 origin master && git checkout -f FETCH_HEAD && git clean -fdx");

        }

        File packagesFile = new File(envPath, "packages.json");

        List<String> mods = (List<String>) Launcher.GSON.fromJson(FileUtil.readUtf8String(packagesFile), List.class);

        String subFn = params[0];

        params = ArrayUtil.remove(params, 0);

        ModuleEnv env = ModuleEnv.get(user.id);

        if (env == null) {

            msg.send("当前有正运行的模块管理程序 :(").async();

            return;

        }

        if ("install".equals(subFn)) {

            if (params.length == 0) {

                msg.send(ArrayUtil.join(mainHelp, "\n")).async();

                return;

            }

            String modName = params[0];

            if (env.modules.containsKey(modName)) {

                NModule mod = env.modules.get(modName);

                if (!mods.contains(modName)) {

                    status.edit("模块仓库无该记录 本地仓库无需更新").async();

                    return;

                }

                HttpResponse result = HttpUtil.createGet(Env.formatRawFile(modName, "package.json")).execute();

                if (!result.isOk()) {

                    status.edit(mod.name + " 检查更新失败 : " + result.getStatus() + "\n\n" + result.body()).async();

                    return;

                }

                NModule syncMod;

                try {

                    syncMod = Launcher.GSON.fromJson(result.body(), NModule.class);

                } catch (Exception ex) {

                    status.edit("检查更新失败 : 模块设定格式错误", BotLog.parseError(ex)).async();

                    return;

                }

                if (syncMod.versionCode <= mod.versionCode) {

                    status.edit("模块 " + mod.name + " 已经是最新版本 [ " + mod.version + " ]", mod.info()).async();

                    return;

                }

                status.edit("正在更新模块 : " + mod.name + " [ " + mod.version + " --> " + syncMod.version + " ] ").async();

                ModuleEnv.exitEnv(user.id);

                executeGitCommand(mod.modPath, GIT_FORCE_UPDATE);

                status.edit("模块已更新 : " + mod.name + " [ " + mod.version + " --> " + syncMod.version + " ] ", syncMod.info()).async();

                ModuleEnv.exiting.remove(user.id);

                return;

            }

            if (!mods.contains(modName)) {

                status.edit("模块源没有该模块 : " + modName).async();

                return;

            }

            HttpResponse result = HttpUtil.createGet(Env.formatRawFile(modName, "package.json")).execute();

            if (!result.isOk()) {

                status.edit(modName + " 元数据获取失败 : " + result.getStatus() + "\n\n" + result.body()).async();

                return;

            }

            NModule mod;

            try {

                mod = Launcher.GSON.fromJson(result.body(), NModule.class);

                mod.modPath = new File(ModuleEnv.mainPath, mod.name);

            } catch (Exception ex) {

                status.edit("元数据获取失败 : 模块设定格式错误", BotLog.parseError(ex)).async();

                return;

            }

            status.edit("正在安装模块 : " + mod.name + " [ " + mod.version + " ] ").async();

            ModuleEnv.exitEnv(user.id);

            executeGitCommand(env.path, "git clone " + Env.MODULES_REPO + "/" + modName);

            status.edit("模块已安装 : " + mod.name + " [ " + mod.version + " ] ", mod.info()).async();

            ModuleEnv.exiting.remove(user.id);

        } else {

            status.edit(mainHelp).async();

        }

    }

}
