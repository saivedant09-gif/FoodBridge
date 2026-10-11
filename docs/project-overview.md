# FoodBridge — Project Overview
## From Surplus to Sustenance: A Database-Driven Intelligent Surplus Food Redistribution & Allocation System

---

## 1. Project Identity & Concept

- **Project Name:** FoodBridge
- **Tagline:** From Surplus to Sustenance
- **Domain:** Social Impact, Sustainability, Database Management Systems (DBMS)
- **Academic Context:** 2nd-Year B.Tech Computer Science and Engineering (Data Science), VIT Vellore
- **Technology Stack:** Java 21, Spring Boot 3.2.5, Spring Security, Spring JDBC / JdbcTemplate, MySQL 8.0, MongoDB 7.0 (Supplementary), Bootstrap 5, Chart.js

---

## 2. Problem Statement

Commercial food providers—such as university cafeterias, restaurants, bakeries, caterers, and event organizers—frequently generate safe, edible surplus food that would otherwise be discarded. Concurrently, registered charities, NGOs, shelters, and community kitchens face resource constraints in feeding underprivileged populations.

### Core Inefficiencies in Existing Workflows:
1. **Uncoordinated Logistics:** Informal messaging (WhatsApp/calls) leads to duplicate claims, miscommunicated deadlines, and uncollected food spoiling before handover.
2. **Double-Allocation / Overselling:** Lack of atomic inventory concurrency permits multiple recipients to claim the same stock simultaneously.
3. **Absence of Verification & Safety Governance:** Unvetted recipients or unaccounted-for food items create safety, liability, and trust barriers for food donors.
4. **Opaque Records:** Lack of centralized transactional records prevents auditing, fulfillment tracking, and impact measurement.

---

## 3. The FoodBridge Solution

FoodBridge bridges the gap between surplus generation and community need via an enterprise full-stack platform:

- **Provider Portal:** Providers log surplus listings with declared quantities, units, preparation timestamps, expiry deadlines, and pickup locations.
- **Recipient Marketplace:** Verified recipients search active inventory with dietary, category, and deadline filters, supported by an explainable rule-based matching engine.
- **Transaction-Safe Allocation Engine:** Spring `@Transactional(isolation = Isolation.SERIALIZABLE)` combined with conditional SQL updates (`UPDATE food_listings SET available_quantity = available_quantity - ? WHERE listing_id = ? AND available_quantity >= ?`) guarantees that inventory is never oversold or made negative.
- **Two-Party Handover Tracking:** Collections require mutual confirmation from both provider and recipient before being recorded as fulfilled.
- **Administrator Governance:** Platform administrators verify organizations, monitor audit logs, manage user statuses, and review reported issues.
- **SQL-Backed Impact Analytics:** Real-time dashboards provide auditable platform metrics (listings, claims, collection completion rates, category distributions) computed directly from relational aggregates.
- **Hybrid Storage Architecture:** MySQL serves as the single source of truth for all transactional data; MongoDB optionally stores flexible metadata (packaging attributes, inspection photos, handling instructions).

---

## 4. Responsible Design & Food Safety Disclaimer

FoodBridge coordinates redistribution logistics; **it does not inspect or certify the biochemical safety of food**. All declarations, allergen notes, and preparation times are user-provided records.

> **University Demonstration Notice:** All data, users, organizations, quantities, and impact metrics seeded into FoodBridge are synthetic demonstration records for academic review and do not represent actual real-world meals served.
