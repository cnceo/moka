package com.cgiser.moka.card.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.cgiser.keel.http.utils.HttpSpringUtils;
import com.cgiser.moka.action.AbstractAction;
import com.cgiser.moka.manager.StageLevelManager;
import com.cgiser.moka.manager.UserCardManager;
import com.cgiser.moka.manager.UserStageManager;
import com.cgiser.moka.model.Role;
import com.cgiser.moka.model.StageLevel;
import com.cgiser.moka.model.UserCard;
import com.cgiser.moka.model.UserStage;
import com.cgiser.moka.result.ReturnType;

public class GetUserCardIdsAction extends AbstractAction  {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response){
		ReturnType<Set<String>> returnType = new ReturnType<Set<String>>();
		try{
			Role role = super.getCurrentRole(request);
			if(null==role){
				returnType.setMsg("您还没登录哦！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			UserCardManager userCardManager = (UserCardManager)HttpSpringUtils.getBean("userCardManager");
			List<UserCard> cards = userCardManager.getUserOwnCard(role.getRoleId());
			Set<String> cardIds = new HashSet<String>();
			for(UserCard userCard : cards){
				cardIds.add(String.valueOf(userCard.getCardId()));
			} 
			UserStageManager userStageManager = (UserStageManager)HttpSpringUtils.getBean("userStageManager");
			List<UserStage> userStages = userStageManager.getUserStageByRoleId(role.getRoleId());
			if(!CollectionUtils.isEmpty(userStages)){
				StageLevelManager stageLevelManager = (StageLevelManager)HttpSpringUtils.getBean("stageLevelManager");
				for(UserStage userStage : userStages){
					List<StageLevel> stageLevels = stageLevelManager.getSageLevelsByStageId(userStage.getStageId());
					for(StageLevel stageLevel : stageLevels){
						String strCardIds[] =  stageLevel.getCardList().split(",");
						for(String id : strCardIds){
							cardIds.add(id.split("_")[0]);
						}
					}
				}
			}
			returnType.setStatus(1);
			returnType.setValue(cardIds);
			super.printReturnType2Response(response, returnType);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return null;
	}
}
