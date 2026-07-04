# Campaign H5 Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade partner campaign sharing into a conversion-focused H5 flow with storefront landing, card item, order confirmation, success voucher, and my-page loop.

**Architecture:** Keep the existing campaign tables and controller. Add duplicate signup protection in `PartnerCampaignController`, then implement the H5 flow inside the existing Vue app using local view state and existing `/campaign/public` APIs.

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 3, Axios, QRCode, Docker canary deployment.

---

### Task 1: Backend Duplicate Signup Protection

**Files:**
- Modify: `src/main/java/com/yaoshizuting/controller/PartnerCampaignController.java`

- [ ] Before inserting a `CampaignSignup`, query by `campaignId`, `mobile`, and `deleted = 0`.
- [ ] If an existing signup is found, return it with `ApiResponse.success(existingSignup)`.
- [ ] Keep current insert behavior for first-time signups.
- [ ] Verify with a repeated POST to `/api/campaign/public/{shareCode}/signup`.

### Task 2: H5 Campaign View State

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] Add state for campaign tab, order remark, current mobile, and submitted signup.
- [ ] Add helper functions for remaining quota, campaign share copy, order flow navigation, and phone dialing.
- [ ] Keep current public API calls and QR generation.

### Task 3: Public H5 Layout

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] Replace the simple campaign public section with a mobile-first H5 shell.
- [ ] Add Home, Card, Confirm, Success, and Mine panels.
- [ ] Add a fixed bottom action/nav area matching the screenshots' interaction model.

### Task 4: Admin Campaign Templates

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] Add four template buttons in the campaign admin form.
- [ ] Populate title, subtitle, price, benefit text, gift text, share reward text, and quota from templates.
- [ ] Preserve manual editing after applying templates.

### Task 5: Verification

**Commands:**
- `mvn -q -DskipTests compile`
- `npm run build` in `frontend`
- Browser smoke test: create/select campaign, open `/?campaign=...`, submit mobile, verify success voucher, repeat submit and verify no duplicate count.
- Deploy canary runtime package to `https://yszt.jilinpc.com` after local verification.
