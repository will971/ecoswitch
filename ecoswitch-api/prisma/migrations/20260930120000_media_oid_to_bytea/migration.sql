-- media_files.data : large object (oid) -> bytea
--
-- Hibernate a mappe le @Lob byte[] en Large Object Postgres. Prisma ne sait pas
-- lire un large object : il n'en verrait que l'oid, pas les octets.
--
-- ATTENTION : ne PAS utiliser le DROP COLUMN / ADD COLUMN que `prisma migrate
-- diff` genere automatiquement — il detruirait toutes les images. La conversion
-- passe obligatoirement par lo_get().

ALTER TABLE "media_files" ADD COLUMN "data_bytea" BYTEA;

UPDATE "media_files" SET "data_bytea" = lo_get("data");

-- Garde-fou : echoue la migration plutot que de perdre une image.
DO $$
DECLARE orphelines INTEGER;
BEGIN
  SELECT count(*) INTO orphelines FROM "media_files" WHERE "data_bytea" IS NULL;
  IF orphelines > 0 THEN
    RAISE EXCEPTION 'Conversion interrompue : % ligne(s) sans octets recuperables', orphelines;
  END IF;
END $$;

-- Libere les large objects maintenant que les octets sont copies.
SELECT lo_unlink("data") FROM "media_files";

ALTER TABLE "media_files" DROP COLUMN "data";
ALTER TABLE "media_files" RENAME COLUMN "data_bytea" TO "data";
ALTER TABLE "media_files" ALTER COLUMN "data" SET NOT NULL;
