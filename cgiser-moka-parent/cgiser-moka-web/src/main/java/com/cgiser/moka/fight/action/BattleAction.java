package com.cgiser.moka.fight.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.cgiser.keel.http.utils.HttpSpringUtils;
import com.cgiser.moka.action.AbstractAction;
import com.cgiser.moka.common.utils.ServletUtil;
import com.cgiser.moka.manager.CardManager;
import com.cgiser.moka.manager.FightManager;
import com.cgiser.moka.manager.RoleManager;
import com.cgiser.moka.manager.SkillManager;
import com.cgiser.moka.model.Battle;
import com.cgiser.moka.model.Card;
import com.cgiser.moka.model.FightCard;
import com.cgiser.moka.model.FightResult;
import com.cgiser.moka.model.FightRune;
import com.cgiser.moka.model.Opp;
import com.cgiser.moka.model.Player;
import com.cgiser.moka.model.Role;
import com.cgiser.moka.model.Round;
import com.cgiser.moka.model.Skill;
import com.cgiser.moka.result.ReturnType;

public class BattleAction extends AbstractAction {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response){
		Logger logger = LoggerFactory.getLogger("fight");
		String battleId = ServletUtil.getDefaultValue(request, "battleid", null);
		String strIsHand = ServletUtil.getDefaultValue(request, "ishand", null);
		String stageId = ServletUtil.getDefaultValue(request, "stageId", null);
		String cardIds = ServletUtil.getDefaultValue(request, "cards", null);
		ReturnType<FightResult> returnType = new ReturnType<FightResult>();
		try{
			Role role = super.getCurrentRole(request);
			if(null==role){
				returnType.setMsg("您还没登录哦,亲！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			if(battleId==null||strIsHand==null||stageId==null){
				returnType.setMsg("参数有误,亲！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			FightManager fightManager = (FightManager)HttpSpringUtils.getBean("fightManager");
			FightResult fightResult = fightManager.getFight(battleId);
			Battle battle = fightResult.getBattle();
			boolean isHand = false;
			if(battle==null){
				returnType.setMsg("战斗不存在哦,亲！");
				returnType.setStatus(0);
				super.printReturnType2Response(response, returnType);
				return null;
			}
			if(strIsHand.equals("1")){
				isHand = true;
			}
			//计算当前回合数
			Round nRound = battle.getRounds().get(battle.getRounds().size()-1);
			int i = nRound.getRound();
			List<Round> rounds = new ArrayList<Round>();
			Round round;
			Opp opp;
			boolean isAttack = true;
			List<Opp> opps;
			boolean flag = false;
			String[] uuids;
			if(cardIds==null){
				uuids = new String[0];
			}else{
				uuids = cardIds.split(",");
			}
			//玩家
			Player aPlayer = fightResult.getAttackPlayer();
			Player dPlayer = fightResult.getDefendPlayer();
			//战斗区域卡牌
			Map<String, FightCard> dFightCards;
			Map<String, FightCard> aFightCards;
			if(fightResult.getDefendPlayer().getFightCards()!=null){
				dFightCards = dPlayer.getFightCards();
			}else{
				dFightCards = new HashMap<String, FightCard>();
				dPlayer.setFightCards(dFightCards);
			}
			if(fightResult.getAttackPlayer().getFightCards()!=null){
				aFightCards = aPlayer.getFightCards();
			}else{
				aFightCards = new HashMap<String, FightCard>();
				aPlayer.setFightCards(aFightCards);
			}
			boolean isFirst = true;
			
			//用户手牌
			int a = aPlayer.getHandsCards().size();
			int b =  dPlayer.getHandsCards().size();

			FightCard fightCard;
			Card card;
			
			//玩家墓地
			List<FightCard> aTumbCards;
			List<FightCard> dTumbCards;
			if(aPlayer.getTumbCards()!=null){
				aTumbCards = aPlayer.getTumbCards();
			}else{
				aTumbCards = new ArrayList<FightCard>();
				aPlayer.setTumbCards(aTumbCards);
			}
			if(dPlayer.getTumbCards()!=null){
				dTumbCards = dPlayer.getTumbCards();
			}else{
				dTumbCards = new ArrayList<FightCard>();
				dPlayer.setTumbCards(dTumbCards);
			}
			//存储新上场卡牌
			List<FightCard> newFightCard = new ArrayList<FightCard>();
			CardManager cardManager = (CardManager)HttpSpringUtils.getBean("cardManager");
			SkillManager skillManager = (SkillManager)HttpSpringUtils.getBean("skillManager");
			while(i>0){
				a = aPlayer.getHandsCards().size();
				b = dPlayer.getHandsCards().size();
				//初始化新的回合
				round = new Round();
				opps = new ArrayList<Opp>();
				round.setRound(i);
				isAttack = i%2==0?false:true;
				if(isFirst){
					isFirst = false;
					//手动战斗上牌到战斗区
					for(int j=0;j<uuids.length;j++){
						if(StringUtils.isBlank(uuids[j])){
							continue;
						}
						for(int c=0;c<aPlayer.getPreCards().size();c++){
							if(aPlayer.getPreCards().get(c).getUUID().equals(uuids[j].trim())){
								newFightCard.add(aPlayer.getPreCards().get(c));
								aPlayer.getFightCards().put(String.valueOf(aPlayer.getFightCards().size()), aPlayer.getPreCards().get(c));
								break;
							}
						}
						for(int f=0;f<newFightCard.size();f++){
							if(aPlayer.getPreCards().contains(newFightCard.get(f))){
								aPlayer.getPreCards().remove(newFightCard.get(f));
							}
						}
					}
					//自动战斗的时候将cd时间为0的卡牌全上场
					if(!isHand){
						for(FightCard fi:aPlayer.getPreCards()){
							if(fi.getWait()<=0){
								opp = new Opp();
								opp.setOpp(1002);
								opp.setUUID(fi.getUUID());
								opp.setValue(0);
								opps.add(opp);
								newFightCard.add(fi);
								aPlayer.getFightCards().put(String.valueOf(aPlayer.getFightCards().size()), fi);
		  
							}
						}
						for(int j=0;j<newFightCard.size();j++){
							if(aPlayer.getPreCards().contains(newFightCard.get(j))){
								aPlayer.getPreCards().remove(newFightCard.get(j));
							}
						}
					}
					
				}else{
					//大于50回合双方英雄开始减血
					if(i>50){
						if(isAttack){
							aPlayer.setHP(aPlayer.getHP()-50);
							opp = new Opp();
							opp.setOpp(1022);
							opp.setTarget(aPlayer.getName());
							opp.setValue(-50);
							opps.add(opp);
						}else{
							dPlayer.setHP(dPlayer.getHP()-50);
							opp = new Opp();
							opp.setOpp(1022);
							opp.setTarget(dPlayer.getName());
							opp.setValue(-50);
							opps.add(opp);
						}
					}
					//更新CD时间数据操作开始
					for(int j=0;j<aPlayer.getPreCards().size();j++){
						aPlayer.getPreCards().get(j).setWait(aPlayer.getPreCards().get(j).getWait()-1);
						if(aPlayer.getPreCards().get(j).getWait()==0){
							if(isHand){
								flag = true;
							}
	  
						}
					}
					for(int j=dPlayer.getPreCards().size()-1;j>-1;j--){
						dPlayer.getPreCards().get(j).setWait(dPlayer.getPreCards().get(j).getWait()-1);
					}
					
					opp = new Opp();
					opp.setOpp(1021);
					opp.setValue(-1);
					opps.add(opp);
					//更新CD时间操作完成
					//推手牌开始
					if(isAttack){
						if(a>0&&aPlayer.getPreCards().size()<5){
							aPlayer.getPreCards().add(aPlayer.getHandsCards().get(a-1));
							//随机手牌
							opp = new Opp();
							opp.setOpp(1001);
							opp.setUUID(aPlayer.getHandsCards().get(a-1).getUUID());
							opp.setValue(0);
							opps.add(opp);
							aPlayer.getHandsCards().remove(a-1);
							a--;
						}
					}else{
						if(b>0&&dPlayer.getPreCards().size()<5){
							dPlayer.getPreCards().add(dPlayer.getHandsCards().get(b-1));
							//随机手牌
							opp = new Opp();
							opp.setOpp(1001);
							opp.setUUID(dPlayer.getHandsCards().get(b-1).getUUID());
							opp.setValue(0);
							opps.add(opp);
							dPlayer.getHandsCards().remove(b-1);
							b--;
						}
					}
					//卡牌上场
					//新上场卡牌区置空
					newFightCard.clear();
					if(isAttack){
						for(FightCard fi:aPlayer.getPreCards()){
							if(fi.getWait()<=0){
								if(isHand){
									flag = true;
								}else{
									opp = new Opp();
									opp.setOpp(1002);
									opp.setUUID(fi.getUUID());
									opp.setValue(0);
									opps.add(opp);
									newFightCard.add(fi);
									aPlayer.getFightCards().put(String.valueOf(aPlayer.getFightCards().size()), fi);
								}
		  
							}
						}
						for(int j=0;j<newFightCard.size();j++){
							if(aPlayer.getPreCards().contains(newFightCard.get(j))){
								aPlayer.getPreCards().remove(newFightCard.get(j));
							}
						}
					}else{
						for(FightCard fi:dPlayer.getPreCards()){
							if(fi.getWait()<=0){
								opp = new Opp();
								opp.setOpp(1002);
								opp.setUUID(fi.getUUID());
								opp.setValue(0);
								opps.add(opp);
								newFightCard.add(fi);
								dPlayer.getFightCards().put(String.valueOf(dPlayer.getFightCards().size()), fi);
		  
							}
						}
						for(int j=0;j<newFightCard.size();j++){
							if(dPlayer.getPreCards().contains(newFightCard.get(j))){
								dPlayer.getPreCards().remove(newFightCard.get(j));
							}
						}
					}
				}                                                                                                                                                                                                                                 
				if(flag){
					round.setIsAttack(isAttack);
					round.setOpps(opps);
					rounds.add(round);
					break;
				}
				//星辰是否还有发动次数
				if(isAttack){
					for(int j=0;j<aPlayer.getRunes().size();j++){
						FightRune fightRune = aPlayer.getRunes().get(j);
						if(fightRune.getRemainTimes()>0){
							List<Opp> oppsRunes = skillManager.getRuneOpps(fightRune, aPlayer, dPlayer,i);
							if(!CollectionUtils.isEmpty(oppsRunes)){
								opps.addAll(oppsRunes);
								opp = new Opp();
								opp.setOpp(50);
								opp.setUUID(fightRune.getuUID());
								opp.setValue(fightRune.getRemainTimes());
								opps.add(opp);
								fightRune.setRemainTimes(fightRune.getRemainTimes()-1);
							}else{
								opp = new Opp();
								opp.setOpp(50);
								opp.setUUID(fightRune.getuUID());
								opp.setValue(-1);
								opps.add(opp);
							}
							if(fightRune.getRemainTimes()<=0){
								opp = new Opp();
								opp.setOpp(1050);
								opp.setUUID(fightRune.getuUID());
								opp.setTarget(fightRune.getuUID());
								opp.setValue(0);
								opps.add(opp);
							}
						}else{
							opp = new Opp();
							opp.setOpp(1050);
							opp.setUUID(fightRune.getuUID());
							opp.setTarget(fightRune.getuUID());
							opp.setValue(0);
							opps.add(opp);
						}
					}
				}else{
					for(int j=0;j<dPlayer.getRunes().size();j++){
						FightRune fightRune = dPlayer.getRunes().get(j);
						if(fightRune.getRemainTimes()>0){
							List<Opp> oppsRunes = skillManager.getRuneOpps(fightRune, dPlayer, aPlayer,i);
							if(!CollectionUtils.isEmpty(oppsRunes)){
								opps.addAll(oppsRunes);
								opp = new Opp();
								opp.setOpp(50);
								opp.setUUID(fightRune.getuUID());
								opp.setValue(fightRune.getRemainTimes());
								opps.add(opp);
								fightRune.setRemainTimes(fightRune.getRemainTimes()-1);
							}else{
								opp = new Opp();
								opp.setOpp(50);
								opp.setUUID(fightRune.getuUID());
								opp.setValue(-1);
								opps.add(opp);
							}
							if(fightRune.getRemainTimes()<=0){
								opp = new Opp();
								opp.setOpp(1050);
								opp.setUUID(fightRune.getuUID());
								opp.setValue(0);
								opps.add(opp);
							}
						}else{
							opp = new Opp();
							opp.setOpp(1050);
							opp.setUUID(fightRune.getuUID());
							opp.setValue(-1);
							opps.add(opp);
						}
					}
				}
				//卡牌上场时查看场上卡牌是否有buff技能
				if(newFightCard.size()>0){
					if(isAttack){
						opps.addAll(skillManager.getSkillModelInCardToPlay(aPlayer, dPlayer, newFightCard));
					}else{
						opps.addAll(skillManager.getSkillModelInCardToPlay(dPlayer, aPlayer, newFightCard));
					}
				}
				//新上场卡牌是否有上场技能
				if(newFightCard.size()>0){
					for(int j=0;j<newFightCard.size();j++){
						fightCard = newFightCard.get(j);
						card = cardManager.getCardById(new Long(fightCard.getCardId()));
						
						if(card.getSkill()>0){
							Skill skill = skillManager.getSkillById(card.getSkill());
							List<Opp> opps1 = null;
							if(isAttack){
								opps1 = skillManager.getSkillModelByType(skill, aPlayer, dPlayer, fightCard);
							}else{
								opps1 = skillManager.getSkillModelByType(skill, dPlayer, aPlayer, fightCard);
							}
							if(opps1!=null){
								opps.addAll(opps1);
							}
						}
						if(fightCard.getLevel()>=5){
							Skill skill = skillManager.getSkillById(card.getLockSkill1());
							List<Opp> opps1 = null;
							if(isAttack){
								opps1 = skillManager.getSkillModelByType(skill, aPlayer, dPlayer, fightCard);
							}else{
								opps1 = skillManager.getSkillModelByType(skill, dPlayer, aPlayer, fightCard);
							}
							if(opps1!=null){
								opps.addAll(opps1);
							}
						}
						if(fightCard.getLevel()>=10){
							Skill skill = skillManager.getSkillById(card.getLockSkill2());
							List<Opp> opps1 = null;
							if(isAttack){
								opps1 = skillManager.getSkillModelByType(skill, aPlayer, dPlayer, fightCard);
							}else{
								opps1 = skillManager.getSkillModelByType(skill, dPlayer, aPlayer, fightCard);
							}
							if(opps1!=null){
								opps.addAll(opps1);
							}
						}

					}
					aPlayer.setFightCards(this.resetFightCard(aPlayer.getFightCards()));
					dPlayer.setFightCards(this.resetFightCard(dPlayer.getFightCards()));
					//更新卡牌区域和战斗区域
					opp = new Opp();
					opp.setOpp(1060);
					opp.setValue(3);
					opps.add(opp);
					opp = new Opp();
					opp.setOpp(1060);
					opp.setValue(3);
					opps.add(opp);
					if(isWin(aPlayer, dPlayer)){
						break;
					}
				}

				if(isAttack){
					for(int j=0;j<=this.getLastIndexFromFightregion(aPlayer.getFightCards());j++){
						List<Skill> skills = new ArrayList<Skill>();
						fightCard = aPlayer.getFightCards().get(String.valueOf(j));
						if(fightCard==null){
							continue;
						}
						card = cardManager.getCardById(new Long(fightCard.getCardId()));
						
						if(card.getSkill()>0){
							Skill skill = skillManager.getSkillById(card.getSkill());
							skills.add(skill);
						}
						if(fightCard.getLevel()>=5){
							Skill skill = skillManager.getSkillById(card.getLockSkill1());							skills.add(skill);
						}
						if(fightCard.getLevel()>=10){
							Skill skill = skillManager.getSkillById(card.getLockSkill2());
							skills.add(skill);
						}
						if(fightCard.getSkillNew()!=null){
							skills.addAll(fightCard.getSkillNew());
						}
						opps.addAll(skillManager.getActiveSkillModelByType(skills, aPlayer, dPlayer, fightCard,fightCard.getSkillBuff(),this.isNewCard(newFightCard, fightCard)));
						if(isWin(aPlayer, dPlayer)){
							break;
						}
					}
					for(int j=0;j<=this.getLastIndexFromFightregion(dPlayer.getFightCards());j++){
						fightCard = dPlayer.getFightCards().get(String.valueOf(j)); 
						if(fightCard==null){
							continue;
						}
						fightCard.getSkillNew().clear();
					}
				}else{
					for(int j=0;j<=this.getLastIndexFromFightregion(dPlayer.getFightCards());j++){
						List<Skill> skills = new ArrayList<Skill>();
						fightCard = dPlayer.getFightCards().get(String.valueOf(j)); 
						if(fightCard==null){
							continue;
						}
						card = cardManager.getCardById(new Long(fightCard.getCardId()));
						
						if(card.getSkill()>0){
							Skill skill = skillManager.getSkillById(card.getSkill());
							skills.add(skill);
						}
						if(fightCard.getLevel()>=5){
							Skill skill = skillManager.getSkillById(card.getLockSkill1());
							skills.add(skill);
						}
						if(fightCard.getLevel()>=10){
							Skill skill = skillManager.getSkillById(card.getLockSkill2());
							skills.add(skill);
						}
						if(fightCard.getSkillNew()!=null){
							skills.addAll(fightCard.getSkillNew());
						}
						opps.addAll(skillManager.getActiveSkillModelByType(skills, dPlayer, aPlayer, fightCard,fightCard.getSkillBuff(),this.isNewCard(newFightCard, fightCard)));
						if(isWin(aPlayer, dPlayer)){
							break;
						}
					}
					for(int j=0;j<=this.getLastIndexFromFightregion(aPlayer.getFightCards());j++){
						fightCard = aPlayer.getFightCards().get(String.valueOf(j));
						if(fightCard==null){
							continue;
						}
						fightCard.getSkillNew().clear();
					}
				}
				aPlayer.setFightCards(this.resetFightCard(aPlayer.getFightCards()));
				dPlayer.setFightCards(this.resetFightCard(dPlayer.getFightCards()));
				//更新卡牌区域和战斗区域
				opp = new Opp();
				opp.setOpp(1060);
				opp.setValue(3);
				opps.add(opp);
				opp = new Opp();
				opp.setOpp(1060);
				opp.setValue(3);
				opps.add(opp);
				round.setOpps(opps);
				round.setIsAttack(isAttack);
				rounds.add(round);
				i++;
				if(flag){
					break;
				}
				if(isWin(aPlayer, dPlayer)){
					break;
				}
				
			}
			if(this.isWin(aPlayer, dPlayer)){
				//设置角色状态为空闲
				RoleManager roleManager = (RoleManager)HttpSpringUtils.getBean("roleManager");
				roleManager.updateRoleStatus(role.getRoleName(), 0);
				fightManager.setFightResult(aPlayer, dPlayer, fightResult, stageId, role);
			}
			
			battle.setRounds(rounds);
			fightResult.setAttackPlayer(aPlayer);
			fightResult.setBattle(battle);
			fightResult.setDefendPlayer(dPlayer);
			fightManager.saveFight(fightResult);
			returnType.setStatus(1);
			returnType.setValue(fightResult);
			super.printReturnType2Response(response, returnType);
			
		}catch (Exception e) {
			logger.error("fight error:",e);
		}

		return null;
	}

	private boolean isWin(Player aPlayer,Player dPlayer){
		if(aPlayer.getHP()<=0||(aPlayer.getFightCards().size()==0&&aPlayer.getPreCards().size()==0&&aPlayer.getHandsCards().size()==0)){
			return true;
		}
		if(dPlayer.getHP()<=0||(dPlayer.getFightCards().size()==0&&dPlayer.getPreCards().size()==0&&dPlayer.getHandsCards().size()==0)){
			return true;
		}
		return false;
	}
	private boolean isNewCard(List<FightCard> fightCards,FightCard fightCard){
		for(int i=0;i<fightCards.size();i++){
			if(fightCards.get(i).getUUID().equals(fightCard.getUUID())){
				return true;
			}
		}
		return false;
	}
	public Map<String,FightCard> resetFightCard(Map<String,FightCard> fightCards){
		int j=0;
		int s=0;
		Map<String, FightCard> fightCards1 = new HashMap<String, FightCard>();
		while(!fightCards.isEmpty()){
			if(fightCards.get(String.valueOf(j))!=null){
				fightCards1.put(String.valueOf(s), fightCards.get(String.valueOf(j)));
				fightCards.remove(String.valueOf(j));
				s++;
			}
			j++;
		}
		return fightCards1;
	}
	private int getLastIndexFromFightregion(Map<String,FightCard> fightCards){
		String j = "0";
		Iterator<String> ite = fightCards.keySet().iterator();
		String key;
		while(ite.hasNext()){
			key =ite.next();
			if(Integer.parseInt(key)>Integer.parseInt(j)){
				j=key;
			}
		}
		return Integer.parseInt(j)+1;
	}
	
}
