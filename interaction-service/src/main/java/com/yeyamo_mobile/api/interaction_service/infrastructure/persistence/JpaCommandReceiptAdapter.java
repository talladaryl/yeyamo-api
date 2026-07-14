package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.interaction_service.application.port.CommandReceiptPort;import com.yeyamo_mobile.api.interaction_service.domain.model.CommandReceipt;
@Component
public class JpaCommandReceiptAdapter implements CommandReceiptPort{
 private final SpringCommandReceiptRepository repo;public JpaCommandReceiptAdapter(SpringCommandReceiptRepository r){repo=r;}public Optional<CommandReceipt>find(String k,String a,String o){return repo.findByIdempotencyKeyAndActorIdAndOperation(k,a,o).map(this::domain);}
 public CommandReceipt save(CommandReceipt r){CommandReceiptEntity e=new CommandReceiptEntity();e.setId(r.id());e.setIdempotencyKey(r.idempotencyKey());e.setActorId(r.actorId());e.setOperation(r.operation());e.setResultId(r.resultId());e.setChanged(r.changed());e.setCreatedAt(r.createdAt());return domain(repo.save(e));}
 private CommandReceipt domain(CommandReceiptEntity e){return new CommandReceipt(e.getId(),e.getIdempotencyKey(),e.getActorId(),e.getOperation(),e.getResultId(),e.isChanged(),e.getCreatedAt());}}
