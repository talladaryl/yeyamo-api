package com.yeyamo_mobile.api.culture_service.domain;

import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecipeDetailsEntityTest {
 @Test void synchronizationPreservesStableIdsAcrossReordering(){CultureContent content=CultureContent.create(ContentType.RECIPE,"recipe","fr-CM","CM","owner",ContributorType.USER,SourceType.PERSONAL_CONTRIBUTION,SensitivityLevel.PUBLIC,Visibility.PUBLIC);RecipeDetailsEntity.RecipeIngredient first=new RecipeDetailsEntity.RecipeIngredient(null,"A","1",null,null,false,null,BigDecimal.ONE,null,QuantityUnitCode.UNIT);RecipeDetailsEntity.RecipeIngredient second=new RecipeDetailsEntity.RecipeIngredient(null,"B","2",null,null,false,null,new BigDecimal("2"),null,QuantityUnitCode.UNIT);RecipeDetailsEntity original=RecipeDetailsEntity.create(content,List.of(first,second),List.of(new RecipeDetailsEntity.RecipeStep("Cook")),10,2);content.synchronizeRecipe(original);RecipeDetailsEntity candidate=RecipeDetailsEntity.create(content,List.of(new RecipeDetailsEntity.RecipeIngredient(second.getId(),"B","2",null,null,false,null,new BigDecimal("2"),null,QuantityUnitCode.UNIT),new RecipeDetailsEntity.RecipeIngredient(first.getId(),"A","1",null,null,false,null,BigDecimal.ONE,null,QuantityUnitCode.UNIT)),List.of(new RecipeDetailsEntity.RecipeStep(original.getSteps().getFirst().getId(),null,"Cook gently",null,null,null,null,null,null,null)),10,2);content.synchronizeRecipe(candidate);assertEquals(List.of(second.getId(),first.getId()),content.getRecipeDetails().getIngredients().stream().map(RecipeDetailsEntity.RecipeIngredient::getId).toList());assertEquals(original.getSteps().getFirst().getId(),content.getRecipeDetails().getSteps().getFirst().getId());}
 @Test void rejectsDescendingQuantityRange(){assertThrows(IllegalArgumentException.class,()->new RecipeDetailsEntity.RecipeIngredient(null,"A",null,null,null,false,null,new BigDecimal("3"),new BigDecimal("2"),QuantityUnitCode.UNIT));}
}
