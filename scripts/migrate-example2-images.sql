-- Chạy trên database ứng dụng sau khi Hibernate tạo product_images.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.products', 'images') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        INSERT INTO dbo.product_images (product_id, image_url, is_primary, display_order, created_at)
        SELECT p.id, p.images,
               CASE WHEN EXISTS (SELECT 1 FROM dbo.product_images i WHERE i.product_id = p.id AND i.is_primary = 1)
                    THEN 0 ELSE 1 END,
               COALESCE((SELECT MAX(i.display_order) + 1 FROM dbo.product_images i WHERE i.product_id = p.id), 0),
               COALESCE(p.created_at, SYSDATETIME())
        FROM dbo.products p
        WHERE p.images IS NOT NULL AND LTRIM(RTRIM(p.images)) <> ''''
          AND NOT EXISTS (SELECT 1 FROM dbo.product_images i WHERE i.product_id = p.id AND i.image_url = p.images);

        UPDATE p SET images = NULL
        FROM dbo.products p
        WHERE EXISTS (SELECT 1 FROM dbo.product_images i WHERE i.product_id = p.id AND i.image_url = p.images);
    ';
END;

COMMIT TRANSACTION;
