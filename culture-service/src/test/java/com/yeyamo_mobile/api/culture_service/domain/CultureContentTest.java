package com.yeyamo_mobile.api.culture_service.domain;

import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CultureContentTest {
 @Test void enforcesWorkflow(){CultureContent c=content(ContentType.PROVERB,"wisdom");c.submit("user");c.changeStatus(ContentStatus.UNDER_REVIEW);c.changeStatus(ContentStatus.APPROVED);c.changeStatus(ContentStatus.PUBLISHED);assertEquals(ContentStatus.PUBLISHED,c.getStatus());}
 @Test void sensitiveContentRequiresVerification(){CultureContent c=CultureContent.create(ContentType.RITUAL,"restricted","fr-CM","CM","user",ContributorType.USER,SourceType.COMMUNITY_ELDER,SensitivityLevel.SACRED,Visibility.PRIVATE);c.submit("user");c.changeStatus(ContentStatus.UNDER_REVIEW);c.changeStatus(ContentStatus.APPROVED);assertThrows(IllegalStateException.class,()->c.changeStatus(ContentStatus.PUBLISHED));}
 @Test void storesStructuredProverbDetails(){CultureContent c=content(ContentType.PROVERB,"wisdom-2");Language language=mock(Language.class);when(language.getCode()).thenReturn("basaa");c.replaceDetails(ProverbDetailsEntity.create(c,"Literal","Meaning",language,null),null);assertEquals("Literal",c.getProverbDetails().getLiteralTranslation());assertNull(c.getRecipeDetails());}
 @Test void storesStructuredRecipeDetails(){CultureContent c=content(ContentType.RECIPE,"ndole");c.replaceDetails(null,RecipeDetailsEntity.create(c,List.of(new RecipeDetailsEntity.RecipeIngredient("Arachides","250","g")),List.of(new RecipeDetailsEntity.RecipeStep("Cook")),30,4));assertEquals(1,c.getRecipeDetails().getIngredients().size());assertEquals("Cook",c.getRecipeDetails().getSteps().getFirst().getInstruction());}
 private CultureContent content(ContentType type,String slug){return CultureContent.create(type,slug,"fr-CM","CM","user",ContributorType.USER,SourceType.ORAL_TESTIMONY,SensitivityLevel.PUBLIC,Visibility.PUBLIC);}
}
