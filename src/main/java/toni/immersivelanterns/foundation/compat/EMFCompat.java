package toni.immersivelanterns.foundation.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.parts.EMFModelPart;

public class EMFCompat {
    public static <M extends LivingEntity> void transformToModelPart(PoseStack matrices, PlayerModel<M> playerModel, double x, double y, double z) {
        var emf = (IEMFModel) playerModel;
        if (!emf.emf$isEMFModel())
        {
            AccessoryRenderer.transformToModelPart(matrices, playerModel.body, x, y, z);
            return;
        }

        var body = emf.emf$getEMFRootModel().getChild("body");
        if (body instanceof EMFModelPart emfPart) {
            if (emfPart.hasChild("EMF_body"))
            {
                var emfChild = emfPart.getChild("EMF_body");
                playerModel.body.translateAndRotate(matrices);
                AccessoryRenderer.transformToModelPart(matrices, emfChild, x, y, z);
                matrices.translate(0, 1.7f, 0);
            }
            else {
                AccessoryRenderer.transformToModelPart(matrices, playerModel.body, x, y, z);
            }
        }
        else {
            AccessoryRenderer.transformToModelPart(matrices, playerModel.body, x, y, z);
        }
    }
}
