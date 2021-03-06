package com.cgiser.moka.legion.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cgiser.keel.http.utils.HttpSpringUtils;
import com.cgiser.moka.action.AbstractAction;
import com.cgiser.moka.manager.LegionManager;
import com.cgiser.moka.model.Legion;
import com.cgiser.moka.model.LegionContext;
import com.cgiser.moka.model.Legioner;
import com.cgiser.moka.model.Role;
import com.cgiser.moka.result.EmblemResult;
import com.cgiser.moka.result.ReturnType;

public class UpgradeEmblemAction extends AbstractAction {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response){
		ReturnType<EmblemResult> returnType = new ReturnType<EmblemResult>();
		try{
			Role role = super.getCurrentRole(request);
			if(null==role){
				returnType.setMsg("您还没登录哦！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			LegionManager legionManager = (LegionManager)HttpSpringUtils.getBean("legionManager");
			Legioner legioner = legionManager.getLegioner(role.getRoleId());
			if(legioner==null){
				returnType.setMsg("您还没加入军团哦！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			Legion legion  = legionManager.getLegionById(legioner.getLegionId());
			if(legion==null){
				returnType.setMsg("帮派不存在！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			if(role.getCash()<LegionContext.LegionEmblemLevelCash[legion.getEmblemLevel()-1]){
				returnType.setMsg("升级军徽所需的元宝不够！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
//			if(legioner.getDuty()!=1&&legioner.getDuty()!=2){
//				returnType.setMsg("您没有权限哦！");
//				returnType.setStatus(0);
//				super.printReturnType2Response(response, returnType);
//				return null;
//			}
			if(legionManager.upLegionEmblem(role, legion.getId())<1){
				returnType.setMsg("升级军徽失败！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			EmblemResult  result = new EmblemResult();
			result.setCash(LegionContext.LegionEmblemLevelCash[legion.getEmblemLevel()-1]);
			legion  = legionManager.getLegionById(legioner.getLegionId());
			legioner = legionManager.getLegioner(role.getRoleId());
			result.setContribute1(legion.getContribute1());
			result.setHonor(legioner.getHonor());
			result.setMyContribute(legioner.getContribute());
			returnType.setValue(result);
			returnType.setStatus(1);
			super.printReturnType2Response(response, returnType);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}
}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      