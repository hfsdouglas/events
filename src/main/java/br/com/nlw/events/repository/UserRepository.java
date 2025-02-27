package br.com.nlw.events.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.nlw.events.model.User;

public interface UserRepository extends CrudRepository<User, Integer> {
	public User findByEmail(String email);
}