package com.chunfeng.newnoteblock;

import com.chunfeng.newnoteblock.block.NewNoteBlock;
import com.chunfeng.newnoteblock.network.NotePacketHandler;
import com.chunfeng.newnoteblock.audio.manager.ServerSoundManager;
import com.chunfeng.newnoteblock.network.WEPacketHandler;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewNoteBlockMod implements ModInitializer {
	public static final String MOD_ID = "newnoteblock";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// --- 新物品注册 ---
		NewNoteBlock.registerBlock();
		NewNoteBlock.registerItems();

		// 注册网络包
		NotePacketHandler.registerServerPackets();

		ServerSoundManager.init();

		// 注册批量操作包处理
		WEPacketHandler.registerServerPackets();
	}
}