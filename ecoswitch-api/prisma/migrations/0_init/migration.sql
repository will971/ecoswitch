-- CreateSchema
CREATE SCHEMA IF NOT EXISTS "public";

-- CreateTable
CREATE TABLE "public"."app_user" (
    "id" BIGSERIAL NOT NULL,
    "email" VARCHAR(320) NOT NULL,
    "name" VARCHAR(200) NOT NULL,
    "password_hash" VARCHAR(255),
    "plan" VARCHAR(50) NOT NULL,
    "provider" VARCHAR(20) NOT NULL,
    "role" VARCHAR(20) NOT NULL,

    CONSTRAINT "app_user_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."brands" (
    "id" BIGSERIAL NOT NULL,
    "logo_url" VARCHAR(2000),
    "name" VARCHAR(100) NOT NULL,

    CONSTRAINT "brands_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."finition_motorisations" (
    "id" BIGSERIAL NOT NULL,
    "default_maintenance_cost" DOUBLE PRECISION,
    "estimated_resale_value" DOUBLE PRECISION,
    "monthly_lld" DOUBLE PRECISION,
    "monthly_loa" DOUBLE PRECISION,
    "purchase_price" DOUBLE PRECISION NOT NULL,
    "finition_id" BIGINT NOT NULL,
    "motorisation_id" BIGINT NOT NULL,

    CONSTRAINT "finition_motorisations_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."finitions" (
    "id" BIGSERIAL NOT NULL,
    "image_url" VARCHAR(2000),
    "name" VARCHAR(100) NOT NULL,
    "model_id" BIGINT NOT NULL,

    CONSTRAINT "finitions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."media_files" (
    "id" BIGSERIAL NOT NULL,
    "content_type" VARCHAR(64) NOT NULL,
    "created_at" TIMESTAMPTZ(6) NOT NULL,
    "data" OID NOT NULL,
    "file_name" VARCHAR(128) NOT NULL,
    "folder" VARCHAR(64) NOT NULL,

    CONSTRAINT "media_files_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."motorisations" (
    "id" BIGSERIAL NOT NULL,
    "autonomie_wltp_km" INTEGER,
    "battery_capacity_kwh" DOUBLE PRECISION,
    "conso_thermique_phev" DOUBLE PRECISION,
    "consumption_wltp" DOUBLE PRECISION NOT NULL,
    "fuel_type" VARCHAR(30) NOT NULL,
    "name" VARCHAR(150) NOT NULL,
    "power_hp" INTEGER,
    "model_id" BIGINT NOT NULL,

    CONSTRAINT "motorisations_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."simulation" (
    "id" BIGSERIAL NOT NULL,
    "name" VARCHAR(300) NOT NULL,
    "saved_at" TIMESTAMP(6) NOT NULL,
    "simulation_data" TEXT NOT NULL,
    "user_email" VARCHAR(320) NOT NULL,

    CONSTRAINT "simulation_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."user_vehicle_profile" (
    "id" BIGSERIAL NOT NULL,
    "annual_mileage" INTEGER,
    "consumption" DOUBLE PRECISION,
    "diesel_price" DOUBLE PRECISION,
    "electric_price" DOUBLE PRECISION,
    "fuel_type" VARCHAR(255) NOT NULL,
    "is_default" BOOLEAN NOT NULL,
    "maintenance_cost" DOUBLE PRECISION,
    "name" VARCHAR(255) NOT NULL,
    "petrol_price" DOUBLE PRECISION,
    "resale_value" DOUBLE PRECISION,
    "user_email" VARCHAR(320) NOT NULL,

    CONSTRAINT "user_vehicle_profile_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."vehicle_models" (
    "id" BIGSERIAL NOT NULL,
    "category" VARCHAR(50),
    "image_url" VARCHAR(2000),
    "name" VARCHAR(100) NOT NULL,
    "brand_id" BIGINT NOT NULL,

    CONSTRAINT "vehicle_models_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "public"."vehicule" (
    "id" BIGSERIAL NOT NULL,
    "annual_mileage" INTEGER,
    "brand" VARCHAR(100),
    "consumption" DOUBLE PRECISION,
    "created_by" VARCHAR(320),
    "fuel_type" VARCHAR(255) NOT NULL,
    "generation" VARCHAR(100),
    "maintenance_cost" DOUBLE PRECISION,
    "model" VARCHAR(100),
    "name" VARCHAR(255) NOT NULL,
    "purchase_price" DOUBLE PRECISION,
    "resale_value" DOUBLE PRECISION,
    "url" VARCHAR(2000),
    "version" VARCHAR(300),
    "visibility" VARCHAR(20) NOT NULL,

    CONSTRAINT "vehicule_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "uk1j9d9a06i600gd43uu3km82jw" ON "public"."app_user"("email" ASC);

-- CreateIndex
CREATE UNIQUE INDEX "ukoce3937d2f4mpfqrycbr0l93m" ON "public"."brands"("name" ASC);

-- CreateIndex
CREATE UNIQUE INDEX "uklpw6gqxbqokgjmcfyc2kl7n8p" ON "public"."finition_motorisations"("finition_id" ASC, "motorisation_id" ASC);

-- CreateIndex
CREATE UNIQUE INDEX "uk5yvcwapwvb7pbkabay7dblrby" ON "public"."media_files"("folder" ASC, "file_name" ASC);

-- CreateIndex
CREATE INDEX "idx_simulation_user_email" ON "public"."simulation"("user_email" ASC);

-- AddForeignKey
ALTER TABLE "public"."finition_motorisations" ADD CONSTRAINT "fk1b3qoogjlyrj1kykvwpfuqe5i" FOREIGN KEY ("motorisation_id") REFERENCES "public"."motorisations"("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- AddForeignKey
ALTER TABLE "public"."finition_motorisations" ADD CONSTRAINT "fk23fvqqhutoqb4vdb17dnbk1rq" FOREIGN KEY ("finition_id") REFERENCES "public"."finitions"("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- AddForeignKey
ALTER TABLE "public"."finitions" ADD CONSTRAINT "fk6j5x1rrad79kuwk9mi4772y71" FOREIGN KEY ("model_id") REFERENCES "public"."vehicle_models"("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- AddForeignKey
ALTER TABLE "public"."motorisations" ADD CONSTRAINT "fk53gb6ujecbdcyjv47wvmrpoep" FOREIGN KEY ("model_id") REFERENCES "public"."vehicle_models"("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- AddForeignKey
ALTER TABLE "public"."vehicle_models" ADD CONSTRAINT "fkd47bmoabg5f8dhgme304ex5ov" FOREIGN KEY ("brand_id") REFERENCES "public"."brands"("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

