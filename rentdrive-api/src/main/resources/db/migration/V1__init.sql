-- =============================================================================
-- V1__init.sql — Schéma initial RentDrive
-- =============================================================================

-- ── roles ─────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id          INT             NOT NULL AUTO_INCREMENT,
    name        VARCHAR(20)     NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_roles         PRIMARY KEY (id),
    CONSTRAINT uk_roles_name    UNIQUE (name)
);

-- ── users ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id             CHAR(36)     NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT uk_users_email   UNIQUE (email),
    CONSTRAINT uk_users_phone   UNIQUE (phone),
    INDEX idx_users_status (status)
);

-- ── user_roles (N:M) ──────────────────────────────────────────────────────────
CREATE TABLE user_roles (
    user_id CHAR(36) NOT NULL,
    role_id INT      NOT NULL,
    CONSTRAINT pk_user_roles       PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user  FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role  FOREIGN KEY (role_id) REFERENCES roles(id)  ON DELETE CASCADE
);

-- ── profiles ──────────────────────────────────────────────────────────────────
CREATE TABLE profiles (
    id            CHAR(36)    NOT NULL,
    user_id       CHAR(36)    NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender        VARCHAR(10),
    avatar_url    VARCHAR(500),
    wilaya        VARCHAR(100),
    city          VARCHAR(100),
    address       TEXT,
    CONSTRAINT pk_profiles         PRIMARY KEY (id),
    CONSTRAINT uk_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_profiles_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── stores ────────────────────────────────────────────────────────────────────
CREATE TABLE stores (
    id                  CHAR(36)       NOT NULL,
    owner_id            CHAR(36)       NOT NULL,
    name                VARCHAR(255)   NOT NULL,
    type                VARCHAR(15)    NOT NULL,
    verification_status VARCHAR(15)    NOT NULL DEFAULT 'PENDING',
    description         TEXT,
    logo_url            VARCHAR(500),
    phone               VARCHAR(20),
    address             TEXT,
    city                VARCHAR(100),
    wilaya              VARCHAR(100),
    rating              DECIMAL(3,2)   NOT NULL DEFAULT 0.00,
    review_count        INT            NOT NULL DEFAULT 0,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    CONSTRAINT pk_stores        PRIMARY KEY (id),
    CONSTRAINT uk_stores_owner  UNIQUE (owner_id),
    CONSTRAINT fk_stores_owner  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_stores_wilaya (wilaya),
    INDEX idx_stores_verif  (verification_status)
);

-- ── vehicles ──────────────────────────────────────────────────────────────────
CREATE TABLE vehicles (
    id             CHAR(36)       NOT NULL,
    version        INT            NOT NULL DEFAULT 0,
    store_id       CHAR(36)       NOT NULL,
    brand          VARCHAR(100)   NOT NULL,
    model          VARCHAR(100)   NOT NULL,
    year           SMALLINT       NOT NULL,
    category       VARCHAR(15)    NOT NULL,
    transmission   VARCHAR(15)    NOT NULL,
    fuel_type      VARCHAR(15)    NOT NULL,
    seats          TINYINT        NOT NULL,
    mileage        INT,
    price_per_day  DECIMAL(10,2)  NOT NULL,
    price_per_week DECIMAL(10,2),
    deposit_amount DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    features       JSON,
    description    TEXT,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING_REVIEW',
    latitude       DECIMAL(10,8),
    longitude      DECIMAL(11,8),
    wilaya         VARCHAR(100),
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    CONSTRAINT pk_vehicles      PRIMARY KEY (id),
    CONSTRAINT fk_vehicles_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    INDEX idx_vehicles_category (category),
    INDEX idx_vehicles_status   (status),
    INDEX idx_vehicles_wilaya   (wilaya),
    INDEX idx_vehicles_price    (price_per_day)
);

-- ── vehicle_photos ────────────────────────────────────────────────────────────
CREATE TABLE vehicle_photos (
    id          CHAR(36)    NOT NULL,
    vehicle_id  CHAR(36)    NOT NULL,
    url         VARCHAR(500) NOT NULL,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_vehicle_photos   PRIMARY KEY (id),
    CONSTRAINT fk_photos_vehicle   FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    INDEX idx_photos_vehicle (vehicle_id)
);

-- ── documents (KYC) ───────────────────────────────────────────────────────────
CREATE TABLE documents (
    id               CHAR(36)    NOT NULL,
    user_id          CHAR(36)    NOT NULL,
    type             VARCHAR(30) NOT NULL,
    file_url         VARCHAR(500) NOT NULL,
    status           VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    verified_by      CHAR(36),
    rejection_reason VARCHAR(500),
    expiry_date      DATE,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    CONSTRAINT pk_documents       PRIMARY KEY (id),
    CONSTRAINT fk_documents_user  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_documents_user_type (user_id, type),
    INDEX idx_documents_status    (status)
);

-- ── bookings ──────────────────────────────────────────────────────────────────
CREATE TABLE bookings (
    id                  CHAR(36)      NOT NULL,
    version             INT           NOT NULL DEFAULT 0,
    vehicle_id          CHAR(36)      NOT NULL,
    renter_id           CHAR(36)      NOT NULL,
    store_id            CHAR(36)      NOT NULL,
    start_date          DATE          NOT NULL,
    end_date            DATE          NOT NULL,
    total_days          SMALLINT      NOT NULL,
    price_per_day       DECIMAL(10,2) NOT NULL,
    subtotal            DECIMAL(10,2) NOT NULL,
    commission          DECIMAL(10,2) NOT NULL,
    deposit_amount      DECIMAL(10,2) NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    cancellation_reason VARCHAR(500),
    confirmed_at        DATETIME(6),
    started_at          DATETIME(6),
    completed_at        DATETIME(6),
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT pk_bookings         PRIMARY KEY (id),
    CONSTRAINT fk_bookings_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_renter  FOREIGN KEY (renter_id)  REFERENCES users(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_store   FOREIGN KEY (store_id)   REFERENCES stores(id)   ON DELETE RESTRICT,
    INDEX idx_bookings_vehicle_dates (vehicle_id, start_date, end_date),
    INDEX idx_bookings_renter        (renter_id),
    INDEX idx_bookings_status        (status),
    INDEX idx_bookings_created       (created_at)
);

-- ── payments ──────────────────────────────────────────────────────────────────
CREATE TABLE payments (
    id              CHAR(36)      NOT NULL,
    booking_id      CHAR(36)      NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'DZD',
    method          VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    deposit_status  VARCHAR(15)   NOT NULL DEFAULT 'HELD',
    transaction_ref VARCHAR(255),
    metadata        JSON,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    CONSTRAINT pk_payments          PRIMARY KEY (id),
    CONSTRAINT uk_payments_booking  UNIQUE (booking_id),
    CONSTRAINT fk_payments_booking  FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    INDEX idx_payments_status (status),
    INDEX idx_payments_method (method)
);

-- ── reviews ───────────────────────────────────────────────────────────────────
CREATE TABLE reviews (
    id         CHAR(36)    NOT NULL,
    booking_id CHAR(36)    NOT NULL,
    author_id  CHAR(36)    NOT NULL,
    target_id  CHAR(36)    NOT NULL,
    vehicle_id CHAR(36)    NOT NULL,
    rating     TINYINT     NOT NULL,
    comment    TEXT,
    type       VARCHAR(25) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_reviews               PRIMARY KEY (id),
    CONSTRAINT uk_reviews_booking_type  UNIQUE (booking_id, type),
    CONSTRAINT chk_reviews_rating       CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_reviews_booking       FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_author        FOREIGN KEY (author_id)  REFERENCES users(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_target        FOREIGN KEY (target_id)  REFERENCES users(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_vehicle       FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    INDEX idx_reviews_vehicle (vehicle_id),
    INDEX idx_reviews_target  (target_id)
);

-- =============================================================================
-- SEED — données de référence
-- =============================================================================

INSERT INTO roles (name, description) VALUES
    ('LOCATAIRE', 'Client qui loue des véhicules'),
    ('BAILLEUR',  'Particulier qui met son véhicule en location'),
    ('AGENCE',    'Agence de location professionnelle'),
    ('ADMIN',     'Administrateur de la plateforme');
