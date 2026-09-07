package com.yeyamo_mobile.api.culture_service.application;

import static com.yeyamo_mobile.api.culture_service.application.CultureDtos.*;
import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.yeyamo_mobile.api.culture_service.domain.Language;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CultureContentServiceStructuredDetailsTest {
 @Test void createsProverbWithStructuredDetails(){Languages languages=mock(Languages.class);Language language=mock(Language.class);when(languages.findById("basaa")).thenReturn(Optional.of(language));when(language.getCode()).thenReturn("basaa");ContentResponse response=service(languages).create(request(ContentType.PROVERB,new ProverbDetailsRequest("Literal","Meaning","basaa",null),null),"user",false);assertNotNull(response.proverbDetails());assertEquals("basaa",response.proverbDetails().originLanguageCode());assertNull(response.recipeDetails());}
 @Test void createsRecipeWithStructuredDetails(){ContentResponse response=service(mock(Languages.class)).create(request(ContentType.RECIPE,null,new RecipeDetailsRequest(List.of(new RecipeIngredientRequest("Peanut","250","g")),List.of(new RecipeStepRequest("Cook")),30,4)),"user",false);assertNotNull(response.recipeDetails());assertEquals("Peanut",response.recipeDetails().ingredients().getFirst().name());assertNull(response.proverbDetails());}
 @Test void rejectsDetailsForAnotherContentType(){CultureException error=assertThrows(CultureException.class,()->service(mock(Languages.class)).create(request(ContentType.RECIPE,new ProverbDetailsRequest("Literal","Meaning","basaa",null),null),"user",false));assertEquals("CULTURE_DETAILS_TYPE_MISMATCH",error.getCode());}
 private CultureContentService service(Languages languages){Contents contents=mock(Contents.class);Translations translations=mock(Translations.class);when(contents.existsBySlug(anyString())).thenReturn(false);return new CultureContentService(contents,translations,mock(CultureEventPublisher.class),null,languages);}
 private ContentRequest request(ContentType type,ProverbDetailsRequest proverb,RecipeDetailsRequest recipe){return new ContentRequest(type,"slug-"+UUID.randomUUID(),"fr-CM","CM",null,null,null,null,null,ContributorType.USER,SourceType.ORAL_TESTIMONY,SensitivityLevel.PUBLIC,Visibility.PUBLIC,new TranslationRequest("fr-CM","Title","Summary","Legacy body"),proverb,recipe);}
}
