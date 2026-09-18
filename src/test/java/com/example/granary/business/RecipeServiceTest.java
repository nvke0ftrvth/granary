package com.example.granary.business;

import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.NotLoggedInException;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.exceptions.ResourceNotFoundException;
import com.example.granary.model.Comment;
import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeImage;
import com.example.granary.model.User;
import com.example.granary.repo.BookmarkRepository;
import com.example.granary.repo.CommentRepository;
import com.example.granary.repo.CommentVoteRepository;
import com.example.granary.repo.RecipeImageRepository;
import com.example.granary.repo.RecipeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.repo.BookmarkRepository.BookmarkCountProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RecipeService, refreshed against the master-branch version
 * that adds getMine(), and makes delete() cascade-clean comments/votes/
 * bookmarks before removing the recipe itself.
 *
 * Constructor argument order matches RecipeService's field declaration order
 * (Lombok @RequiredArgsConstructor): recipeRepository, currentUserService,
 * recipeMapper, recipeImageRepository, imageStorageService,
 * commentRepository, commentVoteRepository, bookmarkRepository.
 *
 * Note on delete()'s @Transactional: Mockito unit tests can't verify actual
 * transactional atomicity (that requires a real Spring proxy + transaction
 * manager). What's tested here is the sequence of repository calls the
 * method makes; whether a failure partway through truly rolls back belongs
 * in an integration test against a real database.
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private RecipeMapper recipeMapper;
    @Mock private RecipeImageRepository recipeImageRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private CommentRepository commentRepository;
    @Mock private CommentVoteRepository commentVoteRepository;
    @Mock private BookmarkRepository bookmarkRepository;

    private RecipeService recipeService;
    private Recipe recipe;
    private User owner;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(
                recipeRepository, currentUserService, recipeMapper, recipeImageRepository,
                imageStorageService, commentRepository, commentVoteRepository, bookmarkRepository);

        owner = new User("owner", "owner@test.com", "pw");
        owner.setId(1L);

        recipe = new Recipe();
        recipe.setId(1L);
        recipe.setTitle("Pancakes");
        recipe.setUser(owner);
        recipe.setImages(new ArrayList<>());
    }

    private User otherUser() {
        User other = new User("intruder", "intruder@test.com", "pw");
        other.setId(2L);
        return other;
    }

    //  create
    @Test
    void create_setsCurrentUserAndUpdatedTimestamp() {
        RecipeRequestDto dto = new RecipeRequestDto();
        Recipe mapped = new Recipe();
        RecipeResponseDto response = new RecipeResponseDto();

        when(recipeMapper.toEntity(dto)).thenReturn(mapped);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(recipeRepository.save(mapped)).thenReturn(mapped);
        when(recipeMapper.toResponseDto(mapped)).thenReturn(response);

        assertThat(recipeService.create(dto)).isSameAs(response);

        assertThat(mapped.getUser()).isSameAs(owner);
        assertThat(mapped.getUpdated()).isNotNull();
        verify(recipeRepository).save(mapped);
    }

    @Test
    void create_notLoggedIn_propagatesAndNeverSaves() {
        RecipeRequestDto dto = new RecipeRequestDto();
        when(recipeMapper.toEntity(dto)).thenReturn(new Recipe());
        when(currentUserService.getCurrentUser()).thenThrow(new NotLoggedInException("not logged in"));

        assertThatThrownBy(() -> recipeService.create(dto))
                .isInstanceOf(NotLoggedInException.class);
        verify(recipeRepository, never()).save(any());
    }

    //  getById
    @Test
    void getById_found_returnsMappedDto() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        RecipeResponseDto dto = new RecipeResponseDto();
        when(recipeMapper.toResponseDto(recipe)).thenReturn(dto);

        assertThat(recipeService.getById(1L)).isSameAs(dto);
        verifyNoInteractions(currentUserService); // GET is intentionally public
    }

    @Test
    void getById_notFound_throwsRecipeNotFoundException() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getById(99L))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    // getAll
    @Test
    void getAll_empty_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recipeRepository.findAllOrderByBookmarkCountDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(bookmarkRepository.countByRecipeIdIn(List.of())).thenReturn(List.of());

        Page<RecipeResponseDto> result = recipeService.getAll(0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getAll_mapsEveryRecipe() {
        Recipe second = new Recipe();
        second.setId(2L);
        Pageable pageable = PageRequest.of(0, 20);
        when(recipeRepository.findAllOrderByBookmarkCountDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recipe, second), pageable, 2));
        when(recipeMapper.toResponseDto(any(Recipe.class))).thenReturn(new RecipeResponseDto());
        when(bookmarkRepository.countByRecipeIdIn(List.of(1L, 2L))).thenReturn(List.of());

        Page<RecipeResponseDto> result = recipeService.getAll(0, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(recipeMapper, times(2)).toResponseDto(any());
    }

    @Test
    void getAll_populatesBookmarkCountFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recipeRepository.findAllOrderByBookmarkCountDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));
        when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

        BookmarkCountProjection projection = mock(BookmarkCountProjection.class);
        when(projection.getRecipeId()).thenReturn(1L);
        when(projection.getCount()).thenReturn(3L);
        when(bookmarkRepository.countByRecipeIdIn(List.of(1L))).thenReturn(List.of(projection));

        Page<RecipeResponseDto> result = recipeService.getAll(0, 20);

        assertThat(result.getContent().get(0).getBookmarkCount()).isEqualTo(3L);
    }

    @Test
    void getAll_recipeWithNoBookmarks_defaultsCountToZero() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recipeRepository.findAllOrderByBookmarkCountDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recipe), pageable, 1));
        when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());
        when(bookmarkRepository.countByRecipeIdIn(List.of(1L))).thenReturn(List.of());

        Page<RecipeResponseDto> result = recipeService.getAll(0, 20);

        assertThat(result.getContent().get(0).getBookmarkCount()).isEqualTo(0L);
    }

    // -------------------------------------------------------------- getByTag
    @Test
    void getByTag_returnsMatches() {
        when(recipeRepository.findByTagsContaining("breakfast")).thenReturn(List.of(recipe));
        when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

        assertThat(recipeService.getByTag("breakfast")).hasSize(1);
    }

    @Test
    void getByTag_noMatches_returnsEmptyList() {
        when(recipeRepository.findByTagsContaining("nope")).thenReturn(List.of());
        assertThat(recipeService.getByTag("nope")).isEmpty();
    }

    // getMine
    @Nested
    class GetMine {

        @Test
        void loggedIn_returnsOnlyThatUsersRecipes() {
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeRepository.findByUserUsername("owner")).thenReturn(List.of(recipe));
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            assertThat(recipeService.getMine()).hasSize(1);
            verify(recipeRepository).findByUserUsername("owner");
        }

        @Test
        void notLoggedIn_propagates() {
            when(currentUserService.getCurrentUser()).thenThrow(new NotLoggedInException("not logged in"));

            assertThatThrownBy(() -> recipeService.getMine())
                    .isInstanceOf(NotLoggedInException.class);
            verifyNoInteractions(recipeRepository);
        }

        @Test
        void noRecipes_returnsEmptyList() {
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeRepository.findByUserUsername("owner")).thenReturn(List.of());

            assertThat(recipeService.getMine()).isEmpty();
        }
    }

    @Nested
    class GetByUsername {

        @Test
        void returnsThatUsersRecipes_noAuthRequired() {
            when(recipeRepository.findByUserUsername("owner")).thenReturn(List.of(recipe));
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            assertThat(recipeService.getByUsername("owner")).hasSize(1);
            verifyNoInteractions(currentUserService); // public -- no login needed
        }

        @Test
        void unknownUsername_returnsEmptyList() {
            when(recipeRepository.findByUserUsername("ghost")).thenReturn(List.of());

            assertThat(recipeService.getByUsername("ghost")).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void owner_appliesDtoAndSaves() {
            RecipeRequestDto dto = new RecipeRequestDto();
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            recipeService.update(1L, dto);

            verify(recipeMapper).updateEntityFromDto(dto, recipe);
            verify(recipeRepository).save(recipe);
        }

        @Test
        void nonOwner_throwsAccessDeniedAndNeverSaves() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser());

            assertThatThrownBy(() -> recipeService.update(1L, new RecipeRequestDto()))
                    .isInstanceOf(AccessDeniedException.class);
            verify(recipeRepository, never()).save(any());
            verify(recipeMapper, never()).updateEntityFromDto(any(), any());
        }

        @Test
        void recipeWithNoOwner_throwsAccessDenied() {
            recipe.setUser(null);
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);

            assertThatThrownBy(() -> recipeService.update(1L, new RecipeRequestDto()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void notLoggedIn_propagatesBeforeOwnershipCheck() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenThrow(new NotLoggedInException("not logged in"));

            assertThatThrownBy(() -> recipeService.update(1L, new RecipeRequestDto()))
                    .isInstanceOf(NotLoggedInException.class);
            verify(recipeRepository, never()).save(any());
        }

        @Test
        void notFound_throwsAndNeverChecksOwnership() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.update(1L, new RecipeRequestDto()))
                    .isInstanceOf(RecipeNotFoundException.class);
            verifyNoInteractions(currentUserService);
        }
    }

    // delete
    @Nested
    class Delete {

        @Test
        void owner_withComments_cascadesVotesThenCommentsThenBookmarksThenRecipe() {
            Comment c1 = Comment.builder().id(101L).build();
            Comment c2 = Comment.builder().id(102L).build();
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(commentRepository.findByRecipeIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c1, c2));

            recipeService.delete(1L);

            ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
            verify(commentVoteRepository).deleteByCommentIdIn(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactly(101L, 102L);
            verify(commentRepository).deleteAll(List.of(c1, c2));
            verify(bookmarkRepository).deleteByRecipeId(1L);
            verify(recipeRepository).deleteById(1L);
        }

        @Test
        void owner_noComments_skipsCommentCleanupButStillClearsBookmarks() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(commentRepository.findByRecipeIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

            recipeService.delete(1L);

            verify(commentVoteRepository, never()).deleteByCommentIdIn(any());
            verify(commentRepository, never()).deleteAll(any());
            verify(bookmarkRepository).deleteByRecipeId(1L);
            verify(recipeRepository).deleteById(1L);
        }

        @Test
        void nonOwner_throwsAccessDeniedAndNeverTouchesCommentsOrBookmarks() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser());

            assertThatThrownBy(() -> recipeService.delete(1L))
                    .isInstanceOf(AccessDeniedException.class);
            verify(recipeRepository, never()).deleteById(anyLong());
            verifyNoInteractions(commentRepository, commentVoteRepository, bookmarkRepository);
        }

        @Test
        void notFound_throwsAndNeverDeletes() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.delete(1L))
                    .isInstanceOf(RecipeNotFoundException.class);
            verify(recipeRepository, never()).deleteById(anyLong());
            verifyNoInteractions(commentRepository, commentVoteRepository, bookmarkRepository);
        }
    }

    // uploadImages
    @Nested
    class UploadImages {

        @Test
        void appendsAfterExistingImagesWithIncrementingDisplayOrder() {
            recipe.getImages().add(RecipeImage.builder().displayOrder(0).build());
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(imageStorageService.store(any())).thenReturn("a.png", "b.png");
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            MultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
            MultipartFile f2 = new MockMultipartFile("files", "b.png", "image/png", new byte[]{1});

            recipeService.uploadImages(1L, List.of(f1, f2));

            assertThat(recipe.getImages()).hasSize(3);
            assertThat(recipe.getImages().get(1).getDisplayOrder()).isEqualTo(1);
            assertThat(recipe.getImages().get(2).getDisplayOrder()).isEqualTo(2);
        }

        @Test
        void nonOwner_throwsAccessDeniedBeforeTouchingStorage() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser());
            MultipartFile f = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(imageStorageService);
        }

        @Test
        void recipeNotFound_throwsBeforeTouchingStorage() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
            MultipartFile f = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(RecipeNotFoundException.class);
            verifyNoInteractions(imageStorageService);
        }

        @Test
        void exceedsMaxOfThreeImages_throwsBeforeValidatingAnyFile() {
            recipe.getImages().add(RecipeImage.builder().displayOrder(0).build());
            recipe.getImages().add(RecipeImage.builder().displayOrder(1).build());
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);

            MultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
            MultipartFile f2 = new MockMultipartFile("files", "b.pdf", "application/pdf", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f1, f2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at most 3 images")
                    .hasMessageContaining("currently has 2")
                    .hasMessageContaining("tried to add 2");
            verifyNoInteractions(imageStorageService);
        }

        @Test
        void exactlyAtMaxOfThreeImages_isAccepted() {
            recipe.getImages().add(RecipeImage.builder().displayOrder(0).build());
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(imageStorageService.store(any())).thenReturn("a.png", "b.png");
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            MultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
            MultipartFile f2 = new MockMultipartFile("files", "b.png", "image/png", new byte[]{1});

            recipeService.uploadImages(1L, List.of(f1, f2));

            assertThat(recipe.getImages()).hasSize(3);
        }

        @Test
        void emptyFile_throwsIllegalArgument() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            MultipartFile empty = new MockMultipartFile("files", "a.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(empty)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        void disallowedMimeType_throws() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            MultipartFile pdf = new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(pdf)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        void nullContentType_throws() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            MultipartFile f = new MockMultipartFile("files", "a.png", null, new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void oversizedFile_throwsWithCorrect2MbMessage() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            byte[] big = new byte[3 * 1024 * 1024];
            MultipartFile f = new MockMultipartFile("files", "a.png", "image/png", big);

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2MB");
        }

        @Test
        void fileAtExactly2MbLimit_isAccepted() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(imageStorageService.store(any())).thenReturn("a.png");
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            byte[] exact = new byte[2 * 1024 * 1024];
            MultipartFile f = new MockMultipartFile("files", "a.png", "image/png", exact);

            recipeService.uploadImages(1L, List.of(f));
            assertThat(recipe.getImages()).hasSize(1);
        }

        @Test
        void extensionMismatch_throws() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            MultipartFile f = new MockMultipartFile("files", "a.png", "image/jpeg", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not match");
        }

        @Test
        void jpegExtensionAllowedForJpegContentType() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(imageStorageService.store(any())).thenReturn("a.jpeg");
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            MultipartFile f = new MockMultipartFile("files", "a.jpeg", "image/jpeg", new byte[]{1});

            recipeService.uploadImages(1L, List.of(f));
            assertThat(recipe.getImages()).hasSize(1);
        }

        @Test
        void filenameWithoutExtension_throws() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            MultipartFile f = new MockMultipartFile("files", "noextension", "image/png", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void missingFilename_throws() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            MultipartFile f = new MockMultipartFile("files", "", "image/png", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(f)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valid name");
        }

        @Test
        void secondFileFailsValidation_firstFileIsAlreadyStoredOnDisk() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(imageStorageService.store(any())).thenReturn("a.png");

            MultipartFile good = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
            MultipartFile bad = new MockMultipartFile("files", "b.pdf", "application/pdf", new byte[]{1});

            assertThatThrownBy(() -> recipeService.uploadImages(1L, List.of(good, bad)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(imageStorageService, times(1)).store(any());
        }
    }

    // deleteImage
    @Nested
    class DeleteImage {

        @Test
        void owner_removesFromRecipeAndStorage() {
            RecipeImage image = RecipeImage.builder().id(10L).filename("a.png").build();
            recipe.getImages().add(image);

            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeImageRepository.findByIdAndRecipeId(10L, 1L)).thenReturn(Optional.of(image));

            recipeService.deleteImage(1L, 10L);

            verify(imageStorageService).delete("a.png");
            assertThat(recipe.getImages()).doesNotContain(image);
            verify(recipeRepository).save(recipe);
        }

        @Test
        void nonOwner_throwsAccessDeniedAndTouchesNoImageData() {
            RecipeImage image = RecipeImage.builder().id(10L).filename("a.png").build();
            recipe.getImages().add(image);

            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser());

            assertThatThrownBy(() -> recipeService.deleteImage(1L, 10L))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(recipeImageRepository, imageStorageService);
        }

        @Test
        void recipeNotFound_throwsAndTouchesNothingElse() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.deleteImage(1L, 10L))
                    .isInstanceOf(RecipeNotFoundException.class);
            verifyNoInteractions(currentUserService, recipeImageRepository, imageStorageService);
        }

        @Test
        void imageNotOnRecipe_throwsResourceNotFound() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeImageRepository.findByIdAndRecipeId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.deleteImage(1L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(imageStorageService);
        }
    }

    // reorderImages
    @Nested
    class ReorderImages {

        @Test
        void appliesOrderByPositionInList() {
            RecipeImage img1 = RecipeImage.builder().id(1L).displayOrder(0).build();
            RecipeImage img2 = RecipeImage.builder().id(2L).displayOrder(1).build();
            recipe.getImages().add(img1);
            recipe.getImages().add(img2);

            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            recipeService.reorderImages(1L, List.of(2L, 1L));

            assertThat(img2.getDisplayOrder()).isEqualTo(0);
            assertThat(img1.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        void nonOwner_throwsAccessDeniedAndLeavesOrderUnchanged() {
            RecipeImage img1 = RecipeImage.builder().id(1L).displayOrder(0).build();
            recipe.getImages().add(img1); // owned by `owner`, not the caller

            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(otherUser());

            assertThatThrownBy(() -> recipeService.reorderImages(1L, List.of(1L)))
                    .isInstanceOf(AccessDeniedException.class);

            verify(recipeRepository, never()).save(any());
            assertThat(img1.getDisplayOrder()).isEqualTo(0);
        }

        @Test
        void unknownIdInList_shiftsSubsequentPositionsInstead() {
            RecipeImage img1 = RecipeImage.builder().id(1L).displayOrder(0).build();
            recipe.getImages().add(img1);

            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            recipeService.reorderImages(1L, List.of(999L, 1L));

            assertThat(img1.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        void recipeNotFound_throws() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recipeService.reorderImages(1L, List.of(1L)))
                    .isInstanceOf(RecipeNotFoundException.class);
        }

        @Test
        void emptyList_savesRecipeUnchanged() {
            RecipeImage img1 = RecipeImage.builder().id(1L).displayOrder(0).build();
            recipe.getImages().add(img1);

            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            when(currentUserService.getCurrentUser()).thenReturn(owner);
            when(recipeRepository.save(recipe)).thenReturn(recipe);
            when(recipeMapper.toResponseDto(recipe)).thenReturn(new RecipeResponseDto());

            recipeService.reorderImages(1L, List.of());

            assertThat(img1.getDisplayOrder()).isEqualTo(0);
            verify(recipeRepository).save(recipe);
        }
    }
}
