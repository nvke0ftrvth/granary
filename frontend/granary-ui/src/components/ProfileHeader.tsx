import { useRef, useState } from 'react';
import {
  useUpdateDescriptionMutation,
  useUploadAvatarMutation,
  useDeleteAvatarMutation,
} from '../store/userApi';
import { ALLOWED_AVATAR_TYPES, MAX_AVATAR_SIZE_BYTES, MAX_DESCRIPTION_LENGTH } from '../types/user';
import type { UserProfileDto } from '../types/user';

interface ProfileHeaderProps {
  profile: UserProfileDto;
  // True when the viewer is allowed to edit this profile (its owner, or an admin).
  canEdit: boolean;
}

function formatSize(bytes: number): string {
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

export function ProfileHeader({ profile, canEdit }: ProfileHeaderProps) {
  const [isEditingDescription, setIsEditingDescription] = useState(false);
  const [descriptionDraft, setDescriptionDraft] = useState(profile.description ?? '');
  const [localError, setLocalError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [updateDescription, { isLoading: isSavingDescription }] = useUpdateDescriptionMutation();
  const [uploadAvatar, { isLoading: isUploadingAvatar }] = useUploadAvatarMutation();
  const [deleteAvatar, { isLoading: isDeletingAvatar }] = useDeleteAvatarMutation();

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    setLocalError(null);
    const file = e.target.files?.[0];
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (!file) return;

    if (file.size > MAX_AVATAR_SIZE_BYTES) {
      setLocalError(`${file.name} exceeds the 2MB limit (${formatSize(file.size)}).`);
      return;
    }
    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      setLocalError(`${file.name} is not a supported image type. Use JPEG, PNG, WEBP, or GIF.`);
      return;
    }

    try {
      await uploadAvatar({ username: profile.username, file }).unwrap();
    } catch {
      setLocalError("Couldn't upload that photo. Try again.");
    }
  };

  const handleAvatarRemove = async () => {
    setLocalError(null);
    try {
      await deleteAvatar(profile.username).unwrap();
    } catch {
      setLocalError("Couldn't remove that photo. Try again.");
    }
  };

  const handleDescriptionSave = async () => {
    setLocalError(null);
    try {
      await updateDescription({ username: profile.username, description: descriptionDraft }).unwrap();
      setIsEditingDescription(false);
    } catch {
      setLocalError("Couldn't save that description. Try again.");
    }
  };

  return (
    <div className="profile-header">
      <div className="profile-avatar-wrap">
        {profile.avatarUrl ? (
          <img className="profile-avatar" src={profile.avatarUrl} alt="" />
        ) : (
          <div className="profile-avatar profile-avatar-placeholder">
            {profile.username.charAt(0).toUpperCase()}
          </div>
        )}

        {canEdit && (
          <div className="profile-avatar-actions">
            <label className="image-upload-btn">
              {isUploadingAvatar ? 'Uploading…' : profile.avatarUrl ? 'Change photo' : '+ Add photo'}
              <input
                ref={fileInputRef}
                type="file"
                accept={ALLOWED_AVATAR_TYPES.join(',')}
                hidden
                disabled={isUploadingAvatar}
                onChange={handleAvatarChange}
              />
            </label>
            {profile.avatarUrl && (
              <button
                type="button"
                className="cancel-btn"
                disabled={isDeletingAvatar}
                onClick={handleAvatarRemove}
              >
                Remove photo
              </button>
            )}
          </div>
        )}
      </div>

      <div className="profile-description">
        {isEditingDescription ? (
          <>
            <textarea
              value={descriptionDraft}
              maxLength={MAX_DESCRIPTION_LENGTH}
              rows={3}
              onChange={(e) => setDescriptionDraft(e.target.value)}
            />
            <div className="form-actions">
              <button
                type="button"
                className="submit-btn"
                disabled={isSavingDescription}
                onClick={handleDescriptionSave}
              >
                {isSavingDescription ? 'Saving…' : 'Save'}
              </button>
              <button
                type="button"
                className="cancel-btn"
                onClick={() => {
                  setDescriptionDraft(profile.description ?? '');
                  setIsEditingDescription(false);
                }}
              >
                Cancel
              </button>
            </div>
          </>
        ) : (
          <>
            <p className="profile-description-text">
              {profile.description ||
                (canEdit ? 'No description yet.' : `${profile.username} hasn't written a description yet.`)}
            </p>
            {canEdit && (
              <button type="button" className="row-add" onClick={() => setIsEditingDescription(true)}>
                Edit description
              </button>
            )}
          </>
        )}
      </div>

      {localError && <p className="form-error">{localError}</p>}
    </div>
  );
}
