package com.yeyamo_mobile.api.culture_service.application;

import static com.yeyamo_mobile.api.culture_service.application.CultureDtos.*;
import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.culture_service.domain.*;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CultureRecipeRevisionWorkflowTest {
 @Test void publishedRecipeStaysPublicUntilCandidateApproval(){
  Contents contents=mock(Contents.class);Translations translations=mock(Translations.class);RecipeRevisions revisions=mock(RecipeRevisions.class);AtomicReference<CultureContent> stored=new AtomicReference<>();AtomicReference<CultureTranslation> translation=new AtomicReference<>();AtomicReference<CultureRecipeRevision> revision=new AtomicReference<>();
  when(contents.existsBySlug(anyString())).thenReturn(false);when(contents.save(any())).thenAnswer(call->{stored.set(call.getArgument(0));return call.getArgument(0);});when(translations.save(any())).thenAnswer(call->{translation.set(call.getArgument(0));return call.getArgument(0);});when(translations.findByContentIdOrderByLanguageCode(any())).thenAnswer(call->translation.get()==null?List.of():List.of(translation.get()));when(revisions.save(any())).thenAnswer(call->{revision.set(call.getArgument(0));return call.getArgument(0);});when(revisions.findFirstByContentIdAndStatusInOrderByCreatedAtDesc(any(),any())).thenAnswer(call->Optional.ofNullable(revision.get()));
  CultureContentService service=new CultureContentService(contents,translations,mock(CultureEventPublisher.class),null,null,revisions,new ObjectMapper().findAndRegisterModules());
  ContentResponse created=service.create(request(10,null),"owner",false);CultureContent content=stored.get();content.submit("owner");content.changeStatus(ContentStatus.UNDER_REVIEW);content.changeStatus(ContentStatus.APPROVED);content.changeStatus(ContentStatus.PUBLISHED);when(contents.findById(created.id())).thenReturn(Optional.of(content));when(contents.findByIdAndStatusAndVisibility(created.id(),ContentStatus.PUBLISHED,Visibility.PUBLIC)).thenReturn(Optional.of(content));
  UUID ingredientId=content.getRecipeDetails().getIngredients().getFirst().getId();ContentResponse candidate=service.update(created.id(),request(20,ingredientId),"owner",false);
  assertEquals(10,service.publicGet(created.id()).recipeDetails().prepTimeMinutes());assertEquals(RecipeRevisionStatus.DRAFT,candidate.recipeRevision().status());
  service.submit(created.id(),"owner");service.review(created.id(),new ReviewRequest(ContentStatus.APPROVED,true,"OK"),"moderator");
  assertEquals(20,service.publicGet(created.id()).recipeDetails().prepTimeMinutes());assertEquals(RecipeRevisionStatus.APPROVED,revision.get().getStatus());
 }
 private ContentRequest request(int prep,UUID ingredientId){return new ContentRequest(ContentType.RECIPE,"recipe","fr-CM","CM",null,null,null,null,null,ContributorType.USER,SourceType.PERSONAL_CONTRIBUTION,SensitivityLevel.PUBLIC,Visibility.PUBLIC,new TranslationRequest("fr-CM","Ndole",null,"History"),null,new RecipeDetailsRequest(List.of(new RecipeIngredientRequest(ingredientId,"Leaf","1","kg",null,false,null,null,null,null)),List.of(new RecipeStepRequest("Cook")),prep,4,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null));}
}
