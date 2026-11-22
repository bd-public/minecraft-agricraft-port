package com.agricraft.agricraft.client.gui;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.LangUtils;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.genetic.Chromosome;
import com.agricraft.agricraft.common.inventory.container.SeedAnalyzerMenu;
import com.agricraft.agricraft.common.registry.AgriRegistries;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class SeedAnalyzerScreen extends AbstractContainerScreen<SeedAnalyzerMenu> {

	private final Identifier GUI = Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "textures/gui/seed_analyzer.png");
	private final Component TEXT_SEPARATOR = Component.literal("-");

	private int geneIndex;

	public SeedAnalyzerScreen(SeedAnalyzerMenu screenContainer, Inventory inv, Component title) {
		super(screenContainer, inv, title, 186, 186);
		this.inventoryLabelY = this.imageHeight - 94;
		this.geneIndex = 0;
	}

	private static boolean hoverUpButton(int startX, int startY, int mouseX, int mouseY) {
		return startX + 67 <= mouseX && mouseX <= startX + 67 + 9 && startY + 26 <= mouseY && mouseY <= startY + 26 + 9;
	}

	private static boolean hoverDownButton(int startX, int startY, int mouseX, int mouseY) {
		return startX + 67 <= mouseX && mouseX <= startX + 67 + 9 && startY + 90 <= mouseY && mouseY <= startY + 90 + 9;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		// background
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
		//journal slot
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + 25, topPos + 70, 186, 73, 18, 18, 256, 256);
		// magnifying glass
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + 13, topPos + 25, 0, 186, 56, 56, 256, 256);

		Optional<AgriGenome> optionalGenome = menu.getGenomeToRender();
		if (optionalGenome.isEmpty()) {
			return;
		}
		AgriGenome genome = optionalGenome.get();

		// up/down buttons if there are more than 6 stats genes
		if (genome.getStatChromosomes().size() > 6) {
			int upXOffset = hoverUpButton(leftPos, topPos, mouseX, mouseY) ? 195 : 186;
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + 67, topPos + 26, upXOffset, 91, 9, 9, 256, 256);
			int downXOffset = hoverDownButton(leftPos, topPos, mouseX, mouseY) ? 195 : 186;
			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos + 67, topPos + 90, downXOffset, 100, 9, 9, 256, 256);
		}

		// species gene
		Chromosome<String> species = genome.species();
		Component speciesDomText = LangUtils.plantName(species.dominant());
		Component speciesRecText = LangUtils.plantName(species.recessive());
		int domw = this.font.width(speciesDomText.getString());
		int middle = leftPos + this.imageWidth / 2;
		int sepLength = this.font.width(TEXT_SEPARATOR.getString());
		guiGraphics.text(this.font, TEXT_SEPARATOR, (int) (middle - sepLength / 2F), topPos + 16, 0xFF000000, false);
		guiGraphics.text(this.font, speciesDomText, (int) (middle - domw - sepLength / 2F - 1), topPos + 16, 0xFF000000, false);
		guiGraphics.text(this.font, speciesRecText, (int) (middle + sepLength / 2F + 1), topPos + 16, 0xFF000000, false);
		// stats genes
		int DNA_X = leftPos + 90;
		int yy = topPos + 26;
		int[] lineAmount = {3, 2, 2, 3, 2, 3};
		int[] lineStart = {0, 15, 25, 35, 50, 60};
		List<Chromosome<Integer>> statGenes = genome.getStatChromosomes().stream().toList();
		for (int i = geneIndex, lineIndex = 0; i < Math.min(statGenes.size(), geneIndex + 6); i++, lineIndex++) {
			Chromosome<Integer> chromosome = statGenes.get(i);
			// color lines of the gene
			for (int k = 0; k < lineAmount[lineIndex]; k++) {
				guiGraphics.horizontalLine(DNA_X, DNA_X + 9, topPos + 26 + lineStart[lineIndex] + k * 5, chromosome.gene().getDominantColor());
				guiGraphics.horizontalLine(DNA_X + 9, DNA_X + 9 + 8, topPos + 26 + lineStart[lineIndex] + k * 5, chromosome.gene().getRecessiveColor());
			}
			// text of the gene
			Identifier key = AgriRegistries.GENES.getRegistry().get().getKey(chromosome.gene());
			Component geneText = AgriRegistries.STATS.getRegistry().get().getOptional(key).map(LangUtils::statName).orElse(Component.empty());
			Component domText = Component.literal("" + chromosome.dominant());
			Component recText = Component.literal("" + chromosome.recessive());
			int w = this.font.width(domText.getString());
			guiGraphics.text(this.font, geneText, DNA_X + 36, yy, 0xFF000000, false);
			guiGraphics.text(this.font, domText, DNA_X - w - 1, yy, 0xFF000000, false);
			guiGraphics.text(this.font, recText, DNA_X + 21, yy, 0xFF000000, false);
			yy += this.font.lineHeight + 4;
		}
		// shape of the dna
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, DNA_X, topPos + 26, 186, 0, 19, 73, 256, 256);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.text(this.font, this.title, this.titleLabelX + 5, this.titleLabelY, 0xFF404040, false);
		guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX + 5, this.inventoryLabelY, 0xFF404040, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		Optional<AgriGenome> opt = menu.getGenomeToRender();
		if (opt.isEmpty()) {
			return super.mouseClicked(event, doubleClick);
		}
		int maxIndex = opt.get().getStatChromosomes().size() - 1;
		if (opt.map(agriGenome -> agriGenome.getStatChromosomes().size()).orElse(0) > 6) {
			int startX = (this.width - this.imageWidth) / 2;
			int startY = (this.height - this.imageHeight) / 2;
			if (hoverUpButton(startX, startY, (int) mouseX, (int) mouseY)) {
				if (geneIndex > 0) {
					geneIndex--;
				}
			}
			if (hoverDownButton(startX, startY, (int) mouseX, (int) mouseY)) {
				if (maxIndex - geneIndex > 6) {
					geneIndex++;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		Optional<AgriGenome> opt = menu.getGenomeToRender();
		if (opt.isEmpty()) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int maxIndex = opt.get().getStatChromosomes().size() - 1;
		if (maxIndex > 6) {
			if (scrollY < 0) {
				if (maxIndex - geneIndex > 6) {
					geneIndex++;
				}
			} else if (scrollY > 0) {
				if (geneIndex > 0) {
					geneIndex--;
				}
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

}
