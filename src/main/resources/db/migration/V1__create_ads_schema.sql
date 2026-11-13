CREATE TABLE IF NOT EXISTS ads (
    id UUID NOT NULL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS ad_images (
    ad_id UUID NOT NULL,
    image_order INTEGER NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    CONSTRAINT pk_ad_images PRIMARY KEY (ad_id, image_order),
    CONSTRAINT fk_ad_images_ads FOREIGN KEY (ad_id) REFERENCES ads (id) ON DELETE CASCADE
);
