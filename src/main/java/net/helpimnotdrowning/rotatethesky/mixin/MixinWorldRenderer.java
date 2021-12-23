package net.helpimnotdrowning.rotatethesky.mixin;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.SkyProperties;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
//import net.fabricmc.example.RotateTheSky;
//import net.minecraft.world.dimension.DimensionType;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WorldRenderer.class, priority = 0)
public class MixinWorldRenderer {
	@Shadow private void renderEndSky(MatrixStack matrices) {}
	@Shadow @Final private static Identifier MOON_PHASES;
	@Shadow @Final private static Identifier SUN;
	@Shadow @Final private MinecraftClient client;
	@Shadow private ClientWorld world;
	@Shadow private VertexBuffer darkSkyBuffer;
	@Shadow private VertexBuffer lightSkyBuffer;
	@Shadow private VertexBuffer starsBuffer;
	
	public float skyRotationDegrees = -37.5F;
	
	/**
     * @reason Because I don't know how to target a specific line in a method (or if that's even possible), I just replace the entire method
	 * 		   (using priority 0 to load first, need for Custom Stars compatability so it can inject stuff into here)
     * @author helpimnotdrowning
     */
	@Overwrite
	public void renderSky(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable) {
		runnable.run();
		if (this.client.world.getSkyProperties().getSkyType() == SkyProperties.SkyType.END) {
			this.renderEndSky(matrices);
		} else if (this.client.world.getSkyProperties().getSkyType() == SkyProperties.SkyType.NORMAL) {
			RenderSystem.disableTexture();
			Vec3d vec3d = this.world.method_23777(this.client.gameRenderer.getCamera().getPos(), f);
			float g = (float)vec3d.x;
			float h = (float)vec3d.y;
			float i = (float)vec3d.z;
			BackgroundRenderer.setFogBlack();
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			RenderSystem.depthMask(false);
			RenderSystem.setShaderColor(g, h, i, 1.0F);
			Shader shader = RenderSystem.getShader();
			this.lightSkyBuffer.setShader(matrices.peek().getModel(), matrix4f, shader);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			float[] fs = this.world.getSkyProperties().getFogColorOverride(this.world.getSkyAngle(f), f);
			float s;
			float t;
			float p;
			float q;
			float r;
			if (fs != null) {
				RenderSystem.setShader(GameRenderer::getPositionColorShader);
				RenderSystem.disableTexture();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				matrices.push();
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				s = MathHelper.sin(this.world.getSkyAngleRadians(f)) < 0.0F ? 180.0F : 0.0F;
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(s));
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
				float k = fs[0];
				t = fs[1];
				float m = fs[2];
				Matrix4f matrix4f2 = matrices.peek().getModel();
				bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(matrix4f2, 0.0F, 100.0F, 0.0F).color(k, t, m, fs[3]).next();

				for(int o = 0; o <= 16; ++o) {
					p = (float)o * 6.2831855F / 16.0F;
					q = MathHelper.sin(p);
					r = MathHelper.cos(p);
					bufferBuilder.vertex(matrix4f2, q * 120.0F, r * 120.0F, -r * 40.0F * fs[3]).color(fs[0], fs[1], fs[2], 0.0F).next();
				}

				bufferBuilder.end();
				BufferRenderer.draw(bufferBuilder);
				matrices.pop();
			}

			RenderSystem.enableTexture();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
			matrices.push();
			s = 1.0F - this.world.getRainGradient(f);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, s);
			// THIS LINE!!!!
			matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(this.skyRotationDegrees));
			matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(this.world.getSkyAngle(f) * 360.0F));
			Matrix4f matrix4f3 = matrices.peek().getModel();
			t = 30.0F;
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, SUN);
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f3, -t, 100.0F, -t).texture(0.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f3, t, 100.0F, -t).texture(1.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f3, t, 100.0F, t).texture(1.0F, 1.0F).next();
			bufferBuilder.vertex(matrix4f3, -t, 100.0F, t).texture(0.0F, 1.0F).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			t = 20.0F;
			RenderSystem.setShaderTexture(0, MOON_PHASES);
			int u = this.world.getMoonPhase();
			int v = u % 4;
			int w = u / 4 % 2;
			float x = (float)(v + 0) / 4.0F;
			p = (float)(w + 0) / 2.0F;
			q = (float)(v + 1) / 4.0F;
			r = (float)(w + 1) / 2.0F;
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f3, -t, -100.0F, t).texture(q, r).next();
			bufferBuilder.vertex(matrix4f3, t, -100.0F, t).texture(x, r).next();
			bufferBuilder.vertex(matrix4f3, t, -100.0F, -t).texture(x, p).next();
			bufferBuilder.vertex(matrix4f3, -t, -100.0F, -t).texture(q, p).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			RenderSystem.disableTexture();
			float ab = this.world.method_23787(f) * s;
			if (ab > 0.0F) {
				RenderSystem.setShaderColor(ab, ab, ab, ab);
				BackgroundRenderer.method_23792();
				this.starsBuffer.setShader(matrices.peek().getModel(), matrix4f, GameRenderer.getPositionShader());
				runnable.run();
			}

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			matrices.pop();
			RenderSystem.disableTexture();
			RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
			double d = this.client.player.getCameraPosVec(f).y - this.world.getLevelProperties().getSkyDarknessHeight(this.world);
			if (d < 0.0D) {
				matrices.push();
				matrices.translate(0.0D, 12.0D, 0.0D);
				this.darkSkyBuffer.setShader(matrices.peek().getModel(), matrix4f, shader);
				matrices.pop();
			}

			if (this.world.getSkyProperties().isAlternateSkyColor()) {
				RenderSystem.setShaderColor(g * 0.2F + 0.04F, h * 0.2F + 0.04F, i * 0.6F + 0.1F, 1.0F);
			} else {
				RenderSystem.setShaderColor(g, h, i, 1.0F);
			}

			RenderSystem.enableTexture();
			RenderSystem.depthMask(true);
		}
	}

}
/*
@Mixin(DimensionType.class)
public class ExampleMixin {
	final OptionalLong fixedTime = OptionalLong.empty();
	
	@Overwrite
    public float getSkyAngle(long time) {
		double d = MathHelper.fractionalPart((double)this.fixedTime.orElse(time) / 24000.0D - 0.25D);
		double e = 0.5D - Math.cos(d * 3.141592653589793D) / 2.0D;
		return (float)(d * 2.0D + e) / 3.0F;
	}
	
}
*/