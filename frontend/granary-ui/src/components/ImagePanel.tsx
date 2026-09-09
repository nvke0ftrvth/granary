import { useRef, useState } from 'react';
import { useGetRecipeByIdQuery, useUploadImagesMutation, useDeleteImageMutation } from '../store/recipeApi';
import { MAX_IMAGES_PER_RECIPE, MAX_IMAGE_SIZE_BYTES, ALLOWED_IMAGE_TYPES } from '../types/recipe';

interface ImagePanelProps {
  recipeId: number;
}

function formatSize(bytes: number): string {
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

export function ImagePanel({ recipeId }: ImagePanelProps) {
  const [localError, setLocalError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Live subscription, not a static prop -- so it reflects new/removed
  // photos immediately after the mutations below invalidate this recipe's cache tag.
  const { data: recipe } = useGetRecipeByIdQuery(recipeId);
  const images = recipe?.images ?? [];

  const [uploadImages, { isLoading: isUploading }] = useUploadImagesMutation();
  const [deleteImage, { isLoading: isDeleting }] = useDeleteImageMutation();

  const slotsRemaining = MAX_IMAGES_PER_RECIPE - images.length;

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    setLocalError(null);
    const selected = Array.from(e.target.files ?? []);
    if (selected.length === 0) return;

    // Reset the input so choosing the same file again still fires onChange
    if (fileInputRef.current) fileInputRef.current.value = '';

    if (selected.length > slotsRemaining) {
      setLocalError(
        `This recipe can have at most ${MAX_IMAGES_PER_RECIPE} photos. ` +
          `You have room for ${slotsRemaining} more.`
      );
      return;
    }

    const oversized = selected.filter((f) => f.size > MAX_IMAGE_SIZE_BYTES);
    if (oversized.length > 0) {
      setLocalError(
        `${oversized.map((f) => f.name).join(', ')} exceed${oversized.length === 1 ? 's' : ''} ` +
          `the 2MB limit (${oversized.map((f) => formatSize(f.size)).join(', ')}).`
      );
      return;
    }

    const wrongType = selected.filter((f) => !ALLOWED_IMAGE_TYPES.includes(f.type));
    if (wrongType.length > 0) {
      setLocalError(
        `${wrongType.map((f) => f.name).join(', ')} ${wrongType.length === 1 ? 'is' : 'are'} not a supported image type. Use JPEG, PNG, WEBP, or GIF.`
      );
      return;
    }

    try {
      await uploadImages({ id: recipeId, files: selected }).unwrap();
    } catch {
      setLocalError("Couldn't upload those photos. Check the file(s) and try again.");
    }
  };

  const handleDelete = async (imageId: number) => {
    setLocalError(null);
    try {
      await deleteImage({ recipeId, imageId }).unwrap();
    } catch {
      setLocalError("Couldn't delete that photo. Try again.");
    }
  };

  return (
    <fieldset className="field-group image-panel">
      <legend className="field-label">
        Photos ({images.length}/{MAX_IMAGES_PER_RECIPE})
      </legend>

      {images.length > 0 && (
        <div className="image-grid">
          {[...images]
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((img) => (
              <div className="image-thumb" key={img.id}>
                <img src={img.imageUrl} alt="" />
                <button
                  type="button"
                  className="image-remove"
                  aria-label="Remove photo"
                  disabled={isDeleting}
                  onClick={() => handleDelete(img.id)}
                >
                  ×
                </button>
              </div>
            ))}
        </div>
      )}

      {slotsRemaining > 0 ? (
        <label className="image-upload-btn">
          {isUploading ? 'Uploading…' : `+ Add photo${slotsRemaining > 1 ? 's' : ''}`}
          <input
            ref={fileInputRef}
            type="file"
            accept={ALLOWED_IMAGE_TYPES.join(',')}
            multiple
            hidden
            disabled={isUploading}
            onChange={handleFileChange}
          />
        </label>
      ) : (
        <p className="image-limit-note">Maximum of {MAX_IMAGES_PER_RECIPE} photos reached.</p>
      )}

      <p className="image-hint">JPEG, PNG, WEBP, or GIF · up to 2MB each</p>

      {localError && <p className="form-error">{localError}</p>}
    </fieldset>
  );
}
