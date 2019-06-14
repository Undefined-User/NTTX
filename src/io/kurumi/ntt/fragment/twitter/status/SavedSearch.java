package io.kurumi.ntt.fragment.twitter.status;

import cn.hutool.core.util.ReUtil;
import com.mongodb.client.FindIterable;
import io.kurumi.ntt.db.Data;
import io.kurumi.ntt.fragment.twitter.archive.StatusArchive;

import java.util.LinkedList;

import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class SavedSearch {

    public static Data<SavedSearch> data = new Data<SavedSearch>(SavedSearch.class);

    static {
        {

            data.collection.drop();

        }
    }

    public Long id;
    public Long user;
    public String text;
    public boolean regex = false;
    public long from = -1;
    public long to = -1;
    public long start = -1;
    public long end = -1;
    public long reply = -1;
    public String content;
    public int media = 0;

    public Bson toFilter() {

        String query = regex ? content : ReUtil.escape(content);

        LinkedList<Bson> filters = new LinkedList<>();

        filters.add(regex("text", query));

        if (from != -1) {

            filters.add(eq("from", from));

        }

        if (to != -1) {

            filters.add(eq("inReplyToUserId", to));

        }

        if (start != -1) {

            filters.add(gte("createdAt", start));

        }

        if (end != -1) {

            filters.add(lte("createdAt", end));

        }

        if (media == 1) {

            filters.add(where("this.mediaUrls.length > 0"));

        } else if (media == 2) {

            filters.add(where("this.mediaUrls.length == 0"));

        }

        if (reply != -1) {

            filters.add(or(eq("inReplyToUserId", reply), eq("quotedStatusId", reply)));

        }

        return and(filters);

    }

    public long count() {

        return StatusArchive.data.collection.countDocuments(toFilter());

    }

    public FindIterable<StatusArchive> query(int skip, int size) {

        return StatusArchive.data.collection.find(toFilter()).sort(new Document("_id", -1)).skip(skip).limit(size);

    }

}
