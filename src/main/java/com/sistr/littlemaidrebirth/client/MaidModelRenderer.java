package com.sistr.littlemaidrebirth.client;

import com.sistr.littlemaidrebirth.entity.ITameable;
import com.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.lmml.client.renderer.MultiModelRenderer;
import net.sistr.lmml.maidmodel.IModelCaps;
import net.sistr.lmml.maidmodel.ModelMultiBase;

@OnlyIn(Dist.CLIENT)
public class MaidModelRenderer extends MultiModelRenderer<LittleMaidEntity> {

    public MaidModelRenderer(EntityRendererManager rendererManager) {
        super(rendererManager);
    }

    @Override
    public void syncCaps(LittleMaidEntity entity, ModelMultiBase model, float partialTicks) {
        super.syncCaps(entity, model, partialTicks);
        model.setCapsValue(IModelCaps.caps_aimedBow, entity.getAimingBow());
        model.setCapsValue(IModelCaps.caps_isWait, entity.getMovingState().equals(ITameable.WAIT));
        model.setCapsValue(IModelCaps.caps_isContract, entity.getOwnerId() != null);
    }

}
