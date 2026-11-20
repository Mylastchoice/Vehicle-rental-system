-- M1 / Stage 3: Flyway V1
-- Generated from project's database/vehicle_rental_system.sql backup (2026-11-20).
-- SCHEMA ONLY: 10 tables, 9 sequences, defaults, PK/FK/unique/check constraints.
-- Never run this directly against a database where the tables already exist.
-- For existing DB: use Flyway baseline version 1 (see HUONG_DAN).

CREATE TABLE public.administrators (
    admin_id bigint NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
);


--
-- TOC entry 217 (class 1259 OID 16630)
-- Name: administrators_admin_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.administrators_admin_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4927 (class 0 OID 0)
-- Dependencies: 217
-- Name: administrators_admin_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.administrators_admin_id_seq OWNED BY public.administrators.admin_id;


--
-- TOC entry 231 (class 1259 OID 16755)
-- Name: booking_details; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.booking_details (
    booking_detail_id bigint NOT NULL,
    booking_id bigint NOT NULL,
    vehicle_code character varying(40) NOT NULL,
    pickup_date date NOT NULL,
    return_date date NOT NULL,
    daily_price numeric(14,2) NOT NULL,
    discount_rate numeric(5,4) DEFAULT 0 NOT NULL,
    rental_days integer NOT NULL,
    subtotal numeric(14,2) NOT NULL,
    CONSTRAINT chk_detail_amounts CHECK (((daily_price >= (0)::numeric) AND ((discount_rate >= (0)::numeric) AND (discount_rate <= (1)::numeric)) AND (rental_days > 0) AND (subtotal >= (0)::numeric))),
    CONSTRAINT chk_detail_dates CHECK ((return_date > pickup_date))
);


--
-- TOC entry 230 (class 1259 OID 16754)
-- Name: booking_details_booking_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.booking_details_booking_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4928 (class 0 OID 0)
-- Dependencies: 230
-- Name: booking_details_booking_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.booking_details_booking_detail_id_seq OWNED BY public.booking_details.booking_detail_id;


--
-- TOC entry 229 (class 1259 OID 16725)
-- Name: bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bookings (
    booking_id bigint NOT NULL,
    user_id bigint NOT NULL,
    pickup_branch_id bigint NOT NULL,
    return_branch_id bigint,
    pickup_location character varying(255) NOT NULL,
    pickup_date date NOT NULL,
    return_date date NOT NULL,
    renter_name character varying(150) NOT NULL,
    renter_phone character varying(20) NOT NULL,
    renter_license_number character varying(100) NOT NULL,
    total_amount numeric(14,2) NOT NULL,
    deposit_amount numeric(14,2) NOT NULL,
    payment_method character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'Pending'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_booking_amounts CHECK (((total_amount >= (0)::numeric) AND (deposit_amount >= (0)::numeric) AND (deposit_amount <= total_amount))),
    CONSTRAINT chk_booking_dates CHECK ((return_date > pickup_date)),
    CONSTRAINT chk_booking_status CHECK (((status)::text = ANY ((ARRAY['Pending'::character varying, 'Confirmed'::character varying, 'Picked up'::character varying, 'Completed'::character varying, 'Cancelled'::character varying])::text[]))),
    CONSTRAINT chk_payment_method CHECK (((payment_method)::text = ANY ((ARRAY['ONLINE'::character varying, 'AT_PICKUP'::character varying])::text[])))
);


--
-- TOC entry 228 (class 1259 OID 16724)
-- Name: bookings_booking_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bookings_booking_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4929 (class 0 OID 0)
-- Dependencies: 228
-- Name: bookings_booking_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bookings_booking_id_seq OWNED BY public.bookings.booking_id;


--
-- TOC entry 222 (class 1259 OID 16654)
-- Name: branches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.branches (
    branch_id integer NOT NULL,
    branch_name character varying(150) NOT NULL,
    address text NOT NULL,
    phone character varying(20)
);


--
-- TOC entry 221 (class 1259 OID 16653)
-- Name: branches_branch_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.branches_branch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4930 (class 0 OID 0)
-- Dependencies: 221
-- Name: branches_branch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.branches_branch_id_seq OWNED BY public.branches.branch_id;


--
-- TOC entry 227 (class 1259 OID 16705)
-- Name: cart_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cart_items (
    cart_item_id bigint NOT NULL,
    cart_id bigint NOT NULL,
    vehicle_code character varying(40) NOT NULL,
    pickup_date date NOT NULL,
    return_date date NOT NULL,
    CONSTRAINT chk_cart_dates CHECK ((return_date > pickup_date))
);


--
-- TOC entry 226 (class 1259 OID 16704)
-- Name: cart_items_cart_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cart_items_cart_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4931 (class 0 OID 0)
-- Dependencies: 226
-- Name: cart_items_cart_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cart_items_cart_item_id_seq OWNED BY public.cart_items.cart_item_id;


--
-- TOC entry 220 (class 1259 OID 16643)
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    category_id bigint NOT NULL,
    category_name character varying(100) NOT NULL,
    description text
);


--
-- TOC entry 219 (class 1259 OID 16642)
-- Name: categories_category_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.categories_category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4932 (class 0 OID 0)
-- Dependencies: 219
-- Name: categories_category_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.categories_category_id_seq OWNED BY public.categories.category_id;


--
-- TOC entry 233 (class 1259 OID 16777)
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    payment_id bigint NOT NULL,
    booking_id bigint NOT NULL,
    amount numeric(14,2) NOT NULL,
    payment_type character varying(30) NOT NULL,
    status character varying(30) DEFAULT 'Pending'::character varying NOT NULL,
    transaction_ref character varying(150),
    paid_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_payment_amount CHECK ((amount >= (0)::numeric))
);


--
-- TOC entry 232 (class 1259 OID 16776)
-- Name: payments_payment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4933 (class 0 OID 0)
-- Dependencies: 232
-- Name: payments_payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_payment_id_seq OWNED BY public.payments.payment_id;


--
-- TOC entry 225 (class 1259 OID 16690)
-- Name: rental_carts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rental_carts (
    cart_id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 224 (class 1259 OID 16689)
-- Name: rental_carts_cart_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.rental_carts_cart_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4934 (class 0 OID 0)
-- Dependencies: 224
-- Name: rental_carts_cart_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.rental_carts_cart_id_seq OWNED BY public.rental_carts.cart_id;


--
-- TOC entry 216 (class 1259 OID 16617)
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    user_id bigint NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    date_of_birth date,
    gender character varying(20),
    address text,
    phone character varying(20) NOT NULL,
    license_number character varying(100),
    role character varying(20) DEFAULT 'MEMBER'::character varying NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- TOC entry 215 (class 1259 OID 16616)
-- Name: users_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4935 (class 0 OID 0)
-- Dependencies: 215
-- Name: users_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_user_id_seq OWNED BY public.users.user_id;


--
-- TOC entry 223 (class 1259 OID 16662)
-- Name: vehicles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehicles (
    vehicle_code character varying(40) NOT NULL,
    category_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    license_plate character varying(30),
    image_url text,
    short_description text,
    detailed_description text,
    daily_price numeric(14,2) NOT NULL,
    discount_rate numeric(5,4) DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'Available'::character varying NOT NULL,
    view_count integer DEFAULT 0 NOT NULL,
    rental_count integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_vehicle_counts CHECK (((view_count >= 0) AND (rental_count >= 0))),
    CONSTRAINT chk_vehicle_discount CHECK (((discount_rate >= (0)::numeric) AND (discount_rate <= (1)::numeric))),
    CONSTRAINT chk_vehicle_price CHECK ((daily_price >= (0)::numeric)),
    CONSTRAINT chk_vehicle_status CHECK (((status)::text = ANY ((ARRAY['Available'::character varying, 'Rented'::character varying, 'Maintenance'::character varying])::text[])))
);


--
-- TOC entry 4682 (class 2604 OID 16634)
-- Name: administrators admin_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrators ALTER COLUMN admin_id SET DEFAULT nextval('public.administrators_admin_id_seq'::regclass);


--
-- TOC entry 4696 (class 2604 OID 16758)
-- Name: booking_details booking_detail_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_details ALTER COLUMN booking_detail_id SET DEFAULT nextval('public.booking_details_booking_detail_id_seq'::regclass);


--
-- TOC entry 4693 (class 2604 OID 16728)
-- Name: bookings booking_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings ALTER COLUMN booking_id SET DEFAULT nextval('public.bookings_booking_id_seq'::regclass);


--
-- TOC entry 4685 (class 2604 OID 16793)
-- Name: branches branch_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.branches ALTER COLUMN branch_id SET DEFAULT nextval('public.branches_branch_id_seq'::regclass);


--
-- TOC entry 4692 (class 2604 OID 16708)
-- Name: cart_items cart_item_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items ALTER COLUMN cart_item_id SET DEFAULT nextval('public.cart_items_cart_item_id_seq'::regclass);


--
-- TOC entry 4684 (class 2604 OID 16646)
-- Name: categories category_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories ALTER COLUMN category_id SET DEFAULT nextval('public.categories_category_id_seq'::regclass);


--
-- TOC entry 4698 (class 2604 OID 16780)
-- Name: payments payment_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN payment_id SET DEFAULT nextval('public.payments_payment_id_seq'::regclass);


--
-- TOC entry 4690 (class 2604 OID 16693)
-- Name: rental_carts cart_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_carts ALTER COLUMN cart_id SET DEFAULT nextval('public.rental_carts_cart_id_seq'::regclass);


--
-- TOC entry 4678 (class 2604 OID 16620)
-- Name: users user_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN user_id SET DEFAULT nextval('public.users_user_id_seq'::regclass);


--

-- TOC entry 4718 (class 2606 OID 16641)
-- Name: administrators administrators_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrators
    ADD CONSTRAINT administrators_email_key UNIQUE (email);


--
-- TOC entry 4720 (class 2606 OID 16639)
-- Name: administrators administrators_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrators
    ADD CONSTRAINT administrators_pkey PRIMARY KEY (admin_id);


--
-- TOC entry 4742 (class 2606 OID 16763)
-- Name: booking_details booking_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_details
    ADD CONSTRAINT booking_details_pkey PRIMARY KEY (booking_detail_id);


--
-- TOC entry 4740 (class 2606 OID 16738)
-- Name: bookings bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT bookings_pkey PRIMARY KEY (booking_id);


--
-- TOC entry 4726 (class 2606 OID 16795)
-- Name: branches branches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.branches
    ADD CONSTRAINT branches_pkey PRIMARY KEY (branch_id);


--
-- TOC entry 4736 (class 2606 OID 16711)
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (cart_item_id);


--
-- TOC entry 4722 (class 2606 OID 16652)
-- Name: categories categories_category_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_category_name_key UNIQUE (category_name);


--
-- TOC entry 4724 (class 2606 OID 16650)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (category_id);


--
-- TOC entry 4746 (class 2606 OID 16785)
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (payment_id);


--
-- TOC entry 4748 (class 2606 OID 16787)
-- Name: payments payments_transaction_ref_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_transaction_ref_key UNIQUE (transaction_ref);


--
-- TOC entry 4732 (class 2606 OID 16696)
-- Name: rental_carts rental_carts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_carts
    ADD CONSTRAINT rental_carts_pkey PRIMARY KEY (cart_id);


--
-- TOC entry 4734 (class 2606 OID 16698)
-- Name: rental_carts rental_carts_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_carts
    ADD CONSTRAINT rental_carts_user_id_key UNIQUE (user_id);


--
-- TOC entry 4744 (class 2606 OID 16765)
-- Name: booking_details uq_booking_vehicle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_details
    ADD CONSTRAINT uq_booking_vehicle UNIQUE (booking_id, vehicle_code);


--
-- TOC entry 4738 (class 2606 OID 16713)
-- Name: cart_items uq_cart_vehicle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT uq_cart_vehicle UNIQUE (cart_id, vehicle_code);


--
-- TOC entry 4714 (class 2606 OID 16629)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 4716 (class 2606 OID 16627)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- TOC entry 4728 (class 2606 OID 16678)
-- Name: vehicles vehicles_license_plate_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT vehicles_license_plate_key UNIQUE (license_plate);


--
-- TOC entry 4730 (class 2606 OID 16676)
-- Name: vehicles vehicles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT vehicles_pkey PRIMARY KEY (vehicle_code);


--
-- TOC entry 4754 (class 2606 OID 16801)
-- Name: bookings fk_booking_pickup_branch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fk_booking_pickup_branch FOREIGN KEY (pickup_branch_id) REFERENCES public.branches(branch_id);


--
-- TOC entry 4755 (class 2606 OID 16806)
-- Name: bookings fk_booking_return_branch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fk_booking_return_branch FOREIGN KEY (return_branch_id) REFERENCES public.branches(branch_id);


--
-- TOC entry 4756 (class 2606 OID 16739)
-- Name: bookings fk_booking_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- TOC entry 4752 (class 2606 OID 16714)
-- Name: cart_items fk_cart_item_cart; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES public.rental_carts(cart_id);


--
-- TOC entry 4753 (class 2606 OID 16719)
-- Name: cart_items fk_cart_item_vehicle; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fk_cart_item_vehicle FOREIGN KEY (vehicle_code) REFERENCES public.vehicles(vehicle_code);


--
-- TOC entry 4751 (class 2606 OID 16699)
-- Name: rental_carts fk_cart_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_carts
    ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- TOC entry 4757 (class 2606 OID 16766)
-- Name: booking_details fk_detail_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_details
    ADD CONSTRAINT fk_detail_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- TOC entry 4758 (class 2606 OID 16771)
-- Name: booking_details fk_detail_vehicle; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.booking_details
    ADD CONSTRAINT fk_detail_vehicle FOREIGN KEY (vehicle_code) REFERENCES public.vehicles(vehicle_code);


--
-- TOC entry 4759 (class 2606 OID 16788)
-- Name: payments fk_payment_booking; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES public.bookings(booking_id);


--
-- TOC entry 4749 (class 2606 OID 16796)
-- Name: vehicles fk_vehicle_branch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT fk_vehicle_branch FOREIGN KEY (branch_id) REFERENCES public.branches(branch_id);


--
-- TOC entry 4750 (class 2606 OID 16679)
-- Name: vehicles fk_vehicle_category; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT fk_vehicle_category FOREIGN KEY (category_id) REFERENCES public.categories(category_id);
