package io.kurumi.nt;

import twitter4j.*;
import java.util.*;
import java.lang.reflect.*;

public class NTApi {



    public static String formatUsernName(User u) {

        return "[ " + u.getName() + "] (@" + u.getScreenName() + ")";

    }

    public static long[] longMarge(long[] a, long[] b) {

        LinkedHashSet<Long> set = new LinkedHashSet<>();
        
        for (long sa : a) {
            
            set.add(sa);
            
        }
        
        for (long sb : b) {

            set.add(sb);

        }
        
        long[] ret = new long[set.size()];
       
        System.arraycopy(set.toArray(),0,ret,0,set.size());
        
        
        return ret;
    }

    public static long[] getAllFr(Twitter api) throws TwitterException {

        long[] all = new long[api.verifyCredentials().getFriendsCount()];

        int index = 0;

        IDs ids = api.getFriendsIDs(-1);

        for (long id : ids.getIDs()) {

            all[index] = id;

            index ++;

        }

        while (ids.hasNext()) {

            ids = api.getFriendsIDs(ids.getNextCursor());

            for (long id : ids.getIDs()) {

                all[index] = id;

                index ++;

            }

        }

        return all;

    }

    public static long[] getAllFo(Twitter api) throws TwitterException {

        long[] all = new long[api.verifyCredentials().getFollowersCount()];

        int index = 0;

        IDs ids = api.getFollowersIDs(-1);

        for (long id : ids.getIDs()) {

            all[index] = id;

            index ++;

        }

        while (ids.hasNext()) {

            ids = api.getFollowersIDs(ids.getNextCursor());

            for (long id : ids.getIDs()) {

                all[index] = id;

                index ++;

            }

        }

        return all;

    }

    public static LinkedList<Status> getContextStatus(Twitter api, Status status, long[] target) throws TwitterException {

        Status top = status;

        while (top.getInReplyToStatusId() != -1 || top.getQuotedStatusId() != -1) {

            if (top.getInReplyToStatusId() != -1) {

                Status superStatus =  api.showStatus(top.getInReplyToStatusId());

                if (target == null || Arrays.binarySearch(target, superStatus.getUser().getId()) != -1) {

                    top = superStatus;

                } else break;

            } else {

                Status superStatus = top.getQuotedStatus();

                if (target == null || Arrays.binarySearch(target, superStatus.getUser().getId()) != -1) {

                    top = superStatus;

                } else break;

            }

        }
        
        

        LinkedList<Status> all =  loopReplies(api, top, target);
        
        all.add(top);
        
        return all;
        

    }

    public static LinkedList<Status> loopReplies(Twitter api, Status s, long[] target) throws TwitterException {

        LinkedList<Status> list = new LinkedList<>();

        for (Status ss : getReplies(api, s)) {

            if (target == null || Arrays.binarySearch(target, (long)ss.getUser().getId()) != -1) {

                list.addAll(loopReplies(api, ss, target));

            }

        }

        return list;

    }

    public static LinkedList<Status> getReplies(Twitter api, Status status) throws TwitterException {

        LinkedList<Status> list = new LinkedList<>();

        QueryResult resp = api.search(new Query()
                                      .query("to:" + status
                                             .getUser()
                                             .getScreenName())
                                      .sinceId(status.getId())
                                      .resultType(Query.RECENT));



        for (Status s : resp.getTweets()) {

            if (s.getInReplyToStatusId() == status.getId()
                || s.getQuotedStatusId() == status.getId()) list.add(s);

        }

        while (resp.hasNext()) {

            resp = api.search(resp.nextQuery());

            for (Status s : resp.getTweets()) {

                if (s.getInReplyToStatusId() == status.getId()
                    || s.getQuotedStatusId() == status.getId()) list.add(s);

            }

        }

        return list;


    }

    public static LinkedList<UserList> getLists(Twitter api) throws IllegalStateException, TwitterException {

        LinkedList<UserList> list = new LinkedList<>();

        ResponseList<UserList> ownlists = api.getUserLists(api.getId());
        list.addAll(ownlists);

        PagableResponseList<UserList> sublist = api.getUserListSubscriptions(api.getId(), -1);
        list.addAll(sublist);

        if (sublist.hasNext()) {

            sublist  = api.getUserListSubscriptions(api.getId(), sublist.getNextCursor());
            list.addAll(sublist);

        }

        return list;

    }

    public static void reply(Twitter api, Status status, String contnent) throws TwitterException {

        if (status.getQuotedStatusId() == -1) {

            api.updateStatus(new StatusUpdate(contnent).inReplyToStatusId(status.getId()));

        }

        StringBuilder replyBuilder = new StringBuilder("@" + status.getUser().getScreenName() + " ");

        Status superStatus = status.getQuotedStatus();

        while (superStatus != null) {

            replyBuilder.append("@" + superStatus.getUser().getScreenName() + " ");

        }

        api.updateStatus(new StatusUpdate(replyBuilder.append(contnent).toString()).inReplyToStatusId(status.getId()));

    }

}
