package toni.immersivelanterns;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Constants;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import toni.immersivelanterns.foundation.IPlayerLanternDataAccessor;
import toni.immersivelanterns.foundation.ImmersiveLanternsMixinConfigPlugin;
import toni.immersivelanterns.foundation.config.AllConfigs;

#if mc == "201"
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
#endif

class LanternRenderer implements SimpleAccessoryRenderer {
    @Override
    public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (reference.entity() instanceof Player player && model instanceof PlayerModel<M> playerModel) {
            boolean isWearingArmor = false;
            for (var armor : player.getArmorSlots()) {
                #if mc > "201"
                if (armor.is(ItemTags.LEG_ARMOR) || armor.is(ItemTags.CHEST_ARMOR)) {
                    isWearingArmor = true;
                    break;
                }
                #else
                if (armor.getItem() instanceof ArmorItem armorItem && (armorItem.getEquipmentSlot() == EquipmentSlot.LEGS || armorItem.getEquipmentSlot() == EquipmentSlot.CHEST)) {
                    isWearingArmor = true;
                    break;
                }
                #endif
            }

            var lanternTop = 11f / 16f;

            var xOffset = AllConfigs.client().leftHandedLanterns.get() ? 0.1f : 2f;
            var zOffset = AllConfigs.client().backLanterns.get() ? (isWearingArmor ? -3.1f : -3f) : -1f;

            var hipOffset = isWearingArmor ? new Vec3(xOffset + 0.05f, -1.25f, zOffset + 0.05f) : new Vec3(xOffset - 0.1f, -1.25f, zOffset - 0.1f);

            AccessoryRenderer.transformToModelPart(matrices, playerModel.body, hipOffset.x, hipOffset.y, hipOffset.z);

            matrices.translate(0.5f, lanternTop, 0.5f);

            Vector4f localPosition = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
            localPosition.mulTranspose(matrices.last().pose());

            var hipPosition = new Vec3(localPosition.x(), localPosition.y(), localPosition.z());
            hipPosition = hipPosition.add(player.getPosition(partialTicks));

            Vec3 update = Minecraft.getInstance().screen == null && AllConfigs.client().enablePhysics.get()
                ? updatePendulum(player, hipPosition, partialTicks)
                : Vec3.ZERO;

            var xRot = update.z;
            xRot += (Math.min(0, playerModel.rightLeg.xRot / 3)) - (AllConfigs.client().backLanterns.get() ? -0.1f : 0.1f);
            xRot -= playerModel.body.xRot;

            matrices.mulPose((new Quaternionf()).rotationZYX((float) update.x, 0f, (float) xRot));
            matrices.translate(-0.5f, -lanternTop, -0.5f);

            var blockstate = Block.byItem(stack.getItem()).defaultBlockState();
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockstate, matrices, multiBufferSource, light, OverlayTexture.NO_OVERLAY);

        }
    }

    Vec3 updatePendulum(Player player, Vec3 newPos, double dt) {
        if (ImmersiveLanternsMixinConfigPlugin.isIrisRenderingShadows())
            return Vec3.ZERO;

        var lanternData = (IPlayerLanternDataAccessor) player;

        var forceStrength = AllConfigs.client().bounciness.get();
        var wasCrouching = lanternData.immersiveLanterns$getWasCrouching();
        var isCrouching = player.isCrouching();
        lanternData.immersiveLanterns$setWasCrouching(isCrouching);

        var zAngle = lanternData.immersiveLanterns$getZAngle();
        var zVel = lanternData.immersiveLanterns$getZVel();

        var xAngle = lanternData.immersiveLanterns$getXAngle();
        var xVel = lanternData.immersiveLanterns$getXVel();

        if (!wasCrouching && isCrouching) {
            xAngle = Constants.PI / 8;
            zAngle = Constants.PI / 10;
        }

        var oldPos = lanternData.immersiveLanterns$getLastHipPosition();
        if (oldPos == null)
            oldPos = newPos;

        var delta = newPos.subtract(oldPos);
        var deltaForward = transformToPlayerRelativeMovement(delta, player.getForward());

        var zForce = 9.81f * Math.sin(zAngle);
        zForce += deltaForward.z * forceStrength * -50;
        zForce += deltaForward.y * forceStrength * -20;
        var zAccel = (-1 * zForce);
        zVel += (float) (zAccel * (dt / 20));
        zVel *= 0.98f;
        zAngle += (float) (zVel * (dt / 20));
        zAngle = Math.max(-Constants.PI / 3, Math.min(Constants.PI / 3, zAngle));

        var xForce = 9.81f * Math.sin(xAngle);
        xForce += deltaForward.x * forceStrength * -50;
        xForce += deltaForward.y * forceStrength * -20;
        var xAccel = (-1 * xForce);
        xVel += (float) (xAccel * (dt / 20));
        xVel *= 0.98f;
        xAngle += (float) (xVel * (dt / 20));
        xAngle = Math.max(-Constants.PI / 3, Math.min(Constants.PI / 3, xAngle));

//        var sb = new StringBuilder();
//
//        sb.append("oldPos: [ ");
//        sb.append(String.format("%.03f", oldPos.x)); sb.append(" | ");
//        sb.append(String.format("%.03f", oldPos.y)); sb.append(" | ");
//        sb.append(String.format("%.03f", oldPos.z));
//
//        sb.append(" ], ");
//
//        sb.append("newPos: [ ");
//        sb.append(String.format("%.03f", newPos.x)); sb.append(" | ");
//        sb.append(String.format("%.03f", newPos.y)); sb.append(" | ");
//        sb.append(String.format("%.03f", newPos.z));
//
//        sb.append(" ], ");
//
//        sb.append("zVel: ");
//        sb.append(String.format("%.02f", zVel));
//        sb.append(", xVel: ");
//        sb.append(String.format("%.02f", xVel));
//
//        ImmersiveLanterns.debugString = sb.toString();

        lanternData.immersiveLanterns$setZVel(Mth.clamp(zVel, -3f, 3f));
        lanternData.immersiveLanterns$setZAngle(zAngle);

        lanternData.immersiveLanterns$setXVel(Mth.clamp(xVel, -3f, 3f));
        lanternData.immersiveLanterns$setXAngle(xAngle);

        lanternData.immersiveLanterns$setLastHipPosition(newPos);

        return new Vec3(
            Math.abs(xAngle) < 0.01f ? 0f : xAngle,
            0f,
            Math.abs(zAngle) < 0.01f ? 0f : zAngle);
    }

    public Vec3 transformToPlayerRelativeMovement(Vec3 deltaMovement, Vec3 forwardVector) {
        // Normalize the forward vector to ensure it has a length of 1
        Vec3 forward = forwardVector.normalize();

        // Compute the right vector by taking the cross product of forward and up vectors
        // Assume the player's up vector is (0, 1, 0) if the player is standing upright
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();

        // Recompute the up vector to ensure it is perpendicular to both forward and right
        up = right.cross(forward).normalize();

        // Now project the world space delta movement onto the local coordinate system
        // The local movement is the dot product of deltaMovement with each of the local axes
        double localForward = deltaMovement.dot(forward);
        double localRight = deltaMovement.dot(right);
        double localUp = deltaMovement.dot(up);

        // Return the movement relative to the player's local space as a new Vec3
        return new Vec3(localRight, localUp, localForward);
    }

    @Override
    public <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, PoseStack matrices) {

    }
}
