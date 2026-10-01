-- Chemin chaud de /api/v1/users/me/vehicle-profiles : toutes les lectures
-- filtrent sur user_email. L'equivalent existe deja sur `simulation`.
CREATE INDEX "user_vehicle_profile_user_email_idx" ON "user_vehicle_profile"("user_email");
