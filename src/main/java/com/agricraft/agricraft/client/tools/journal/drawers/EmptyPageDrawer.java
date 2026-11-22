package com.agricraft.agricraft.client.tools.journal.drawers;

import com.agricraft.agricraft.api.tools.journal.JournalData;
import com.agricraft.agricraft.api.tools.journal.JournalPage;
import com.agricraft.agricraft.api.tools.journal.JournalPageDrawer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class EmptyPageDrawer implements JournalPageDrawer<JournalPage> {

	public static final EmptyPageDrawer INSTANCE = new EmptyPageDrawer();

	@Override
	public void drawLeftSheet(GuiGraphicsExtractor guiGraphics, JournalPage page, int pageX, int pageY, JournalData journalData) {
		guiGraphics.text(Minecraft.getInstance().font, Component.literal("missing journal page drawer: " + page.getDrawerId().toString()), pageX, pageY, 0xFFFFFFFF);
	}

	@Override
	public void drawRightSheet(GuiGraphicsExtractor guiGraphics, JournalPage page, int pageX, int pageY, JournalData journalData) {
		guiGraphics.text(Minecraft.getInstance().font, Component.literal("missing journal page drawer: " + page.getDrawerId().toString()), pageX, pageY, 0xFFFFFFFF);
	}

}
