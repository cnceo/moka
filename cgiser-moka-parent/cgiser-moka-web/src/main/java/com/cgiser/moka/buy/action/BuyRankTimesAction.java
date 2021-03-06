package com.cgiser.moka.buy.action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cgiser.core.common.cache.mem.MemCachedManager;
import com.cgiser.keel.http.utils.HttpSpringUtils;
import com.cgiser.moka.action.AbstractAction;
import com.cgiser.moka.manager.RoleManager;
import com.cgiser.moka.model.LoginAward;
import com.cgiser.moka.model.RankRoleCache;
import com.cgiser.moka.model.Role;
import com.cgiser.moka.result.ReturnType;

public class BuyRankTimesAction extends AbstractAction {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response){
		ReturnType<List<LoginAward>> returnType = new ReturnType<List<LoginAward>>();
		try{
			Role role = super.getCurrentRole(request);
			if(null==role){
				returnType.setMsg("您还没登录哦！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			
			RoleManager roleManager = (RoleManager)HttpSpringUtils.getBean("roleManager");
			if(role.getCash()<10){
				returnType.setMsg("您的元宝不够哦！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			if(roleManager.updateCash(role.getRoleName(), 10)){
				MemCachedManager rankCachedManager = (MemCachedManager)HttpSpringUtils.getBean("rankCachedManager");
				RankRoleCache rankRoleCache = (RankRoleCache)rankCachedManager.get("rank_"+String.valueOf(role.getRoleId()));
				rankRoleCache.setRankHasTimes(rankRoleCache.getRankHasTimes()+1);
				rankCachedManager.set("rank_"+String.valueOf(role.getRoleId()), 0, rankRoleCache);
			}else{
				logger.error("["+role.getRoleName()+"]购买排名战次数失败");
			}
			returnType.setStatus(1);
			super.printReturnType2Response(response, returnType);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}
}
