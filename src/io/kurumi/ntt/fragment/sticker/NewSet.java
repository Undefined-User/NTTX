package io.kurumi.ntt.fragment.sticker;

import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.db.UserData;
import com.pengrad.telegrambot.request.GetStickerSet;
import io.kurumi.ntt.model.request.Keyboard;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.fragment.sticker.NewSet.CreateSet;
import com.pengrad.telegrambot.request.CreateNewStickerSet;
import io.kurumi.ntt.utils.Html;
import com.pengrad.telegrambot.response.GetStickerSetResponse;
import cn.hutool.core.util.StrUtil;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.model.Sticker;
import com.pengrad.telegrambot.request.AddStickerToSet;

public class NewSet extends Fragment {

	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerFunction("newset");
		registerPoint(POINT_CREATE_SET);

	}

	final String POINT_CREATE_SET = "set_create";

	final String CREATE_COPY = "复制已有贴纸包";
	final String CREATE_BY_IMAGE = "使用任意图片创建";
	final String CREATE_BY_STICKER = "使用已有贴纸创建";

	class CreateSet extends PointData {

		int type = 0;

		String name;
		String title;

	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		CreateSet data = new CreateSet();

		setPrivatePoint(user,POINT_CREATE_SET,data);

		msg.send("好，一个新贴纸包 现在请发送标题 :","\n注意 : 虽然NTT支持通过自动重建的方式修改标题,但是还是想好再输入吧 ~").exec(data);

	}

	final String DOC = Html.a("相关法律法规","https://core.telegram.org/bots/api#createnewstickerset");

	@Override
	public void onPoint(UserData user,Msg msg,String point,PointData data) {

		data.context.add(msg);

		CreateSet create = (CreateSet) data;

		if (create.type == 0) {

			if (!msg.hasText()) {

				msg.send("请输入新贴纸包的标题 :").withCancel().exec(data);

				return;

			} else if (msg.text().length() > 64) {

				msg.send("标题太长啦！根据 " + DOC + " 最多64个字 ~").exec(data);

				return;

			}

			create.name = msg.text();
			create.type = 1;

			msg.send("现在发送贴纸集的简称 : 用于添加贴纸的链接 https://t.me/addstickers/你设置的简称 。只能包含英文字母，数字和下划线。必须以字母开头，不能包含连续的下划线。 ","\n并且 : 根据 " + DOC + " , " + Html.b("必须以 '_by_" + origin.me.username().toLowerCase() + "' 结尾。") + " '" + origin.me.username().toLowerCase() + "' 不区分大小写 (不带引号)。").html().exec(data);

		} else if (create.type == 1) {

			if (!msg.hasText()) {

				msg.send("请输入新贴纸包的简称 :").withCancel().exec(data);

				return;

			} else if (msg.text().length() > 64) {

				msg.send("简称太长啦！根据 " + DOC + " 最多64个字 ~").html().exec(data);

				return;

			} else if (!msg.text().toLowerCase().endsWith("_by_" + origin.me.username().toLowerCase())) {

				msg.send("对不起，但是根据 " + DOC + " , " + Html.b("必须以 '_by_" + origin.me.username().toLowerCase() + "' 结尾。") + " '" + origin.me.username().toLowerCase() + "' 不区分大小写 (不带引号)。 :)").html().exec(data);

				return;

			}

			GetStickerSetResponse check = bot().execute(new GetStickerSet(msg.text()));

			if (check != null && check.isOk()) {

				msg.send("对不起，但是这个简称好像已经被使用了 :)").exec(data);

				return;

			}

			create.name = msg.text();
			create.type = 2;

			msg
				.send("好，一个新的贴纸包 选择创建方式 :")
				.keyboard(new Keyboard() {{

						newButtonLine(CREATE_COPY);
						newButtonLine(CREATE_BY_IMAGE);
						newButtonLine(CREATE_BY_STICKER);

					}})
				.withCancel()
				.exec(data);


		} else if (create.type == 2) {

			if (CREATE_COPY.equals(msg.text())) {

				msg.send("现在请发送 目标贴纸包的简称或链接 或目标贴纸包的任意贴纸 : ").withCancel().exec(data);

				create.type = 3;

			} else if (CREATE_BY_IMAGE.equals(msg.text())) {

				msg.send("现在请发送任意图片 (建议使用文件格式 直接发送图片会被压缩) : ").withCancel().exec(data);

				create.type = 4;

			} else if (CREATE_BY_STICKER.equals(msg.text())) {

				msg.send("现在请发送任意贴纸 : ").withCancel().exec(data);

				create.type = 5;

			}

		} else if (create.type == 3) {

			if (!msg.hasText()) {

				msg.send("请发送 目标贴纸包的简称或链接 或目标贴纸包的任意贴纸 : ").withCancel().exec(data);

				return;

			}

			String target = msg.text();

			if (target.contains("/")) target = StrUtil.subAfter(target,"/",true);

			final GetStickerSetResponse set = bot().execute(new GetStickerSet(target));

			if (!set.isOk()) {

				msg.send("无法读取贴纸包 " + target + " : " + set.description()).exec(data);

				return;

			}

			msg.send("正在创建贴纸包...").exec(data);

			BaseResponse resp = bot().execute(new CreateNewStickerSet(user.id.intValue(),create.name,create.title,set.stickerSet().stickers()[0].fileId(),set.stickerSet().stickers()[0].emoji()) {{ 

						if (set.stickerSet().stickers()[0].maskPosition() != null) {

							maskPosition(set.stickerSet().stickers()[0].maskPosition()); 

						}

					}});

			if (!resp.isOk()) {

				msg.send("创建贴纸集失败 请重试 : " + resp.description()).withCancel().exec(data);

				return;

			}

			for (int index = 1;index < set.stickerSet().stickers().length;index ++) {

				final Sticker sticker = set.stickerSet().stickers()[index];

				bot().execute(new AddStickerToSet(user.id.intValue(),create.name,sticker.fileId(),sticker.emoji()) {{
					
					if (sticker.maskPosition() != null) {
						
						maskPosition(sticker.maskPosition());
						
					}
					
				}});

			}
			
			clearPrivatePoint(user);
			
			msg.send("创建成功！ " + Html.a(create.title,"https://t.me/addstickers/" + create.name)).html().exec(data);

		}

	}

}