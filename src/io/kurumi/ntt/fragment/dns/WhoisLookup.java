package io.kurumi.ntt.fragment.dns;

import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import org.neverfear.whois.ResolveDefault;
import org.neverfear.whois.WhoisQuery;

import java.io.IOException;

public class WhoisLookup extends Fragment {

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerFunction("whois");

    }

    @Override
    public int checkFunctionContext(UserData user, Msg msg, String function, String[] params) {

        return FUNCTION_PUBLIC;

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if (params.length == 0) {

            msg.invalidParams("domain").async();

            return;

        }

        String result;

        try {

            result = new WhoisQuery(params[0]).getResponse().getData();

        } catch (Exception e) {

            try {

                result = new ResolveDefault("whois.iana.org").query(params[0]).getData();

            } catch (IOException ex) {

                msg.send(ex.getMessage()).async();

                return;

            }

        }

        String message = null;

        for (String line : result.split("\n")) {

            if (line.trim().startsWith("%")) continue;

            if (message == null) message = line;
            else message += "\n" + line;

            if (line.contains("<<<")) break;

        }

        msg.send(message).async();

    }

}
