package io.kurumi.ntt.fragment.secure;

import cn.hutool.core.codec.*;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.Html;

public class CodecFN extends Fragment {

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerFunction(

                "base32", "base32d",
                "base62", "base62d",
                "base64", "base64d",

                "caesar", "caesard",
                "morse", "morsed",
                "rot", "rotd"

        );

    }

	@Override
	public int checkFunctionContext(UserData user, Msg msg, String function, String[] params) {
		
		return FUNCTION_PUBLIC;
		
	}

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if (StrUtil.isBlank(msg.param())) {

            msg.invalidParams("文本...").async();

            return;

        }


        if (function.startsWith("base32")) {

            if (!function.endsWith("d")) {

                msg.reply(Base32.encode(msg.param())).async();

            } else {

                String result = null;

                try {

                    result = Base32.decodeStr(msg.param());

                } catch (Exception ex) {
                }

                if (StrUtil.isBlank(result)) {

                    msg.reply("解码失败 :)").async();

                } else {

                    msg.reply("结果 : {}", Html.code(result)).html().async();

                }

            }

        } else if (function.startsWith("base62")) {

            if (!function.endsWith("d")) {

                msg.reply(Base62.encode(msg.param())).async();

            } else {

                String result = null;

                try {

                    result = Base62.decodeStr(msg.param());

                } catch (Exception ex) {
                }

                if (StrUtil.isBlank(result)) {

                    msg.reply("解码失败 :)").async();

                } else {

                    msg.reply("结果 : {}", Html.code(result)).html().async();

                }

            }

        } else if (function.startsWith("base64")) {

            if (!function.endsWith("d")) {

                msg.reply(Base64.encode(msg.param())).async();

            } else {

                String result = null;

                try {

                    result = Base64.decodeStr(msg.param());

                } catch (Exception ex) {
                }

                if (StrUtil.isBlank(result)) {

                    msg.reply("解码失败 :)").async();

                } else {

                    msg.reply("结果 : {}", Html.code(result)).html().async();

                }

            }

        } else if (function.startsWith("caesar")) {

            params = msg.params();

            if (params.length < 2 || !NumberUtil.isNumber(params[0])) {

                msg.invalidParams("偏移量", "文本...").async();

                return;

            }

            if (!function.endsWith("d")) {

                msg.reply(Caesar.encode(ArrayUtil.join(ArrayUtil.remove(params, 0), " "), NumberUtil.parseInt(params[0]))).async();

            } else {

                String result = null;

                try {

                    result = Caesar.decode(ArrayUtil.join(ArrayUtil.remove(params, 0), " "), NumberUtil.parseInt(params[0]));

                } catch (Exception ex) {
                }

                if (StrUtil.isBlank(result)) {

                    msg.reply("解码失败 :)").async();

                } else {

                    msg.reply("结果 : {}", Html.code(result)).html().async();

                }

            }

        } else if (function.startsWith("morse")) {

            if (!function.endsWith("d")) {

                msg.reply(new Morse().encode(msg.param())).async();

            } else {

                String result = null;

                try {

                    result = new Morse().decode(msg.param());

                } catch (Exception ex) {
                }

                if (StrUtil.isBlank(result)) {

                    msg.reply("解码失败 :)").async();

                } else {

                    msg.reply("结果 : {}", Html.code(result)).html().async();

                }

            }

        } else if (function.startsWith("rot")) {

            params = msg.params();

            if (params.length < 2 || !NumberUtil.isNumber(params[0])) {

                msg.invalidParams("偏移量", "文本...").async();

                return;

            }

            if (!function.endsWith("d")) {

                msg.reply(Rot.encode(ArrayUtil.join(ArrayUtil.remove(params, 0), " "), NumberUtil.parseInt(params[0]), true)).async();

            } else {

                String result = null;

                try {

                    result = Rot.decode(ArrayUtil.join(ArrayUtil.remove(params, 0), " "), NumberUtil.parseInt(params[0]), true);

                } catch (Exception ex) {
                }

                if (StrUtil.isBlank(result)) {

                    msg.reply("解码失败 :)").async();

                } else {

                    msg.reply("结果 : {}", Html.code(result)).html().async();

                }

            }

        }

    }

}
