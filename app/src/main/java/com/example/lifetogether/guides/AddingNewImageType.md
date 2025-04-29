# Guide to adding new imageType

1. /domain/model/ImageType
   - Update to include new image type
2. /data
   1. /local/LocalDataSource
      - Update getImageByteArray() to handle new type
   2. /remote/FirebaseStorageDataSource
      - Update uploadPhoto() to handle new type
   3. /remote/FirestoreDataSource
      - Update getImageUrl() to handle new type
      - Update saveImageDownloadUrl() to handle new type
      - Update saveImageMetaData() to handle new type
3. /domain/usecase/image/UploadImagesUseCase (*optional*)
   - If adding new images of the type should include metadata (like galleryImages)
   - Then update firestoreNewUrlResult to include new type