package br.com.nlw.events.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.nlw.events.dto.SubscriptionRankingByUser;
import br.com.nlw.events.dto.SubscriptionRankingItem;
import br.com.nlw.events.dto.SubscriptionResponse;
import br.com.nlw.events.exception.EventNotFoundException;
import br.com.nlw.events.exception.SubscriptionConflictException;
import br.com.nlw.events.exception.UserIndicatorNotFoundException;
import br.com.nlw.events.model.Event;
import br.com.nlw.events.model.Subscription;
import br.com.nlw.events.model.User;
import br.com.nlw.events.repository.EventRepository;
import br.com.nlw.events.repository.SubscriptionRepository;
import br.com.nlw.events.repository.UserRepository;

@Service
public class SubscriptionService {
	@Autowired
	private EventRepository evtRepo;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private SubscriptionRepository subRepo;
	
	public SubscriptionResponse createNewSubscription(String eventName, User user, Integer userId) {
		// Recuperar o evento pelo nome
		Event evt = evtRepo.findByPrettyName(eventName);
		
		if (evt == null) {
			throw new EventNotFoundException("Evento" + eventName + "não existe");
		}
		
		User userRes = userRepo.findByEmail(user.getEmail());
		
		if (userRes == null) {
			userRes = userRepo.save(user);
		}
		
		User indicator = null;
		
		if (userId != null) {
			indicator = userRepo.findById(userId).orElse(null);
			
			if (indicator == null) {
				throw new UserIndicatorNotFoundException("Usuário indicador " + userId + " não existe!");
			}
		}
		
		Subscription subs = new Subscription();
		subs.setEvent(evt);
		subs.setSubscriber(userRes);
		
		Subscription tmpSub = subRepo.findByEventAndSubscriber(evt, userRes);
		
		if (tmpSub != null) {
			throw new SubscriptionConflictException("Já existe inscrição para o usuário " + userRes.getName() + "no evento " + evt.getTitle());
		}
		
		Subscription res = subRepo.save(subs);
		
		return new SubscriptionResponse(res.getSubscriptionNumber(), "http://codecraft.com/subscripton/" + res.getEvent().getPrettyName() + "/" + res.getSubscriber().getId());
	}
	
	public List<SubscriptionRankingItem> getCompleteRanking(String prettyName) {
		Event evt = evtRepo.findByPrettyName(prettyName);
		
		if (evt == null) {
			throw new EventNotFoundException("Ranking do evento " + prettyName +" não existe");
		}
		
		return subRepo.generateRanking(evt.getEventId());
	}
	
	public SubscriptionRankingByUser getRankingByUser(String prettyName, Integer userId) {
		List<SubscriptionRankingItem> ranking = getCompleteRanking(prettyName);
		
		SubscriptionRankingItem item = ranking.stream().filter(i->i.userId().equals(userId)).findFirst().orElse(null);
		
		if (item == null) {
			throw new UserIndicatorNotFoundException("Não há inscrições com indicação do usuário " + userId);
		}
		
		Integer position = IntStream.range(0, ranking.size()).filter(pos -> ranking.get(pos).userId().equals(userId)).findFirst().getAsInt();
		
		return new SubscriptionRankingByUser(item, position + 1);
	}
}
