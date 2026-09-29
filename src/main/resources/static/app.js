/**
 * LoyaltyPoints Engine - Single Page Application Logic
 */

(function () {
    'use strict';

    // Application State
    const state = {
        currentView: 'customers',
        customers: [],
        tiers: [],
        config: {
            pointsPerRupee: 0.1,
            rupeesPerPoint: 1.0
        },
        selectedCustomerId: null,
        historyPage: 0,
        historyPageSize: 5,
        historyTotalPages: 1,
        activeTierFilter: 'ALL',
        searchQuery: ''
    };

    // DOM Elements
    const elements = {
        // Nav
        navCustomers: document.getElementById('nav-customers'),
        navPurchase: document.getElementById('nav-purchase'),
        navDetails: document.getElementById('nav-details'),
        customersCountBadge: document.getElementById('customers-count-badge'),

        // Views
        viewCustomers: document.getElementById('view-customers'),
        viewPurchase: document.getElementById('view-purchase'),
        viewDetails: document.getElementById('view-details'),

        // Topbar
        pageTitle: document.getElementById('page-title'),
        pageSubtitle: document.getElementById('page-subtitle'),
        btnRefresh: document.getElementById('btn-refresh'),
        btnOpenAddCustomer: document.getElementById('btn-open-add-customer'),

        // Customers Table
        customersTableBody: document.getElementById('customers-table-body'),
        customerSearchInput: document.getElementById('customer-search-input'),
        tierFilterPills: document.getElementById('tier-filter-pills'),

        // Add Customer Modal
        modalAddCustomer: document.getElementById('modal-add-customer'),
        addCustomerForm: document.getElementById('add-customer-form'),
        btnCloseModal: document.getElementById('btn-close-modal'),
        btnCancelModal: document.getElementById('btn-cancel-modal'),
        custNameInput: document.getElementById('cust-name-input'),
        custEmailInput: document.getElementById('cust-email-input'),
        custPhoneInput: document.getElementById('cust-phone-input'),
        errorName: document.getElementById('error-name'),
        errorEmail: document.getElementById('error-email'),
        errorPhone: document.getElementById('error-phone'),
        modalGeneralError: document.getElementById('modal-general-error'),

        // Purchase View
        purchaseCustomerSelect: document.getElementById('purchase-customer-select'),
        purchaseCustomerInfo: document.getElementById('purchase-customer-info'),
        purchaseAmountInput: document.getElementById('purchase-amount-input'),
        purchaseRedeemInput: document.getElementById('purchase-redeem-input'),
        btnUseMaxPoints: document.getElementById('btn-use-max-points'),
        redeemHint: document.getElementById('redeem-hint'),
        purchaseForm: document.getElementById('purchase-form'),
        purchaseFormError: document.getElementById('purchase-form-error'),
        btnSubmitPurchase: document.getElementById('btn-submit-purchase'),

        // Live Preview
        previewGross: document.getElementById('preview-gross'),
        previewTierName: document.getElementById('preview-tier-name'),
        previewTierRate: document.getElementById('preview-tier-rate'),
        previewTierDiscount: document.getElementById('preview-tier-discount'),
        previewRedeemPts: document.getElementById('preview-redeem-pts'),
        previewRedeemDiscount: document.getElementById('preview-redeem-discount'),
        previewNet: document.getElementById('preview-net'),
        previewPointsEarned: document.getElementById('preview-points-earned'),
        previewNewBalance: document.getElementById('preview-new-balance'),
        previewTierProjection: document.getElementById('preview-tier-projection'),

        // Details View
        detailsCustomerSelect: document.getElementById('details-customer-select'),
        btnQuickPurchaseFromDetails: document.getElementById('btn-quick-purchase-from-details'),
        detailsAvatar: document.getElementById('details-avatar'),
        detailsName: document.getElementById('details-name'),
        detailsEmail: document.getElementById('details-email'),
        detailsPhone: document.getElementById('details-phone'),
        detailsTierBadge: document.getElementById('details-tier-badge'),
        detailsNextTierLabel: document.getElementById('details-next-tier-label'),
        detailsProgressFill: document.getElementById('details-progress-fill'),
        detailsAvailablePts: document.getElementById('details-available-pts'),
        detailsLifetimePts: document.getElementById('details-lifetime-pts'),
        detailsDiscountPct: document.getElementById('details-discount-pct'),
        historyTableBody: document.getElementById('history-table-body'),
        detailsHistoryTotal: document.getElementById('details-history-total'),
        historyPageInfo: document.getElementById('history-page-info'),
        btnHistoryPrev: document.getElementById('btn-history-prev'),
        btnHistoryNext: document.getElementById('btn-history-next'),

        // Celebrations & Toasts
        celebrationBannerContainer: document.getElementById('celebration-banner-container'),
        toastContainer: document.getElementById('toast-container')
    };

    // =========================================================================
    // API Client
    // =========================================================================
    async function apiRequest(endpoint, options = {}) {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                ...options.headers
            },
            ...options
        };

        try {
            const res = await fetch(endpoint, config);
            const data = await res.json().catch(() => null);

            if (!res.ok) {
                const error = new Error((data && data.message) || `HTTP Error ${res.status}`);
                error.status = res.status;
                error.response = data;
                throw error;
            }
            return data;
        } catch (err) {
            console.error(`API Error on ${endpoint}:`, err);
            throw err;
        }
    }

    // =========================================================================
    // Initialization & Data Loading
    // =========================================================================
    async function init() {
        setupEventListeners();
        try {
            await Promise.all([
                loadConfig(),
                loadTiers(),
                loadCustomers()
            ]);
        } catch (err) {
            showToast('error', 'Initialization Failed', 'Could not load initial data from server.');
        }
    }

    async function loadConfig() {
        try {
            const config = await apiRequest('/api/config');
            if (config) {
                state.config = config;
            }
        } catch (e) {
            console.warn('Using default config fallback');
        }
    }

    async function loadTiers() {
        try {
            const tiers = await apiRequest('/api/tiers');
            if (Array.isArray(tiers)) {
                state.tiers = tiers;
            }
        } catch (e) {
            console.error('Failed to load tiers', e);
        }
    }

    async function loadCustomers(preferredSelectedId = null) {
        try {
            const customers = await apiRequest('/api/customers');
            state.customers = Array.isArray(customers) ? customers : [];

            elements.customersCountBadge.textContent = state.customers.length;
            renderCustomersTable();
            populateCustomerDropdowns(preferredSelectedId);

            // If a customer was already selected or requested, update summary
            if (preferredSelectedId) {
                state.selectedCustomerId = preferredSelectedId;
            } else if (!state.selectedCustomerId && state.customers.length > 0) {
                state.selectedCustomerId = state.customers[0].id;
            }

            if (state.currentView === 'details' && state.selectedCustomerId) {
                loadCustomerDetails(state.selectedCustomerId);
            }
        } catch (err) {
            showToast('error', 'Error', 'Failed to fetch customer list');
            elements.customersTableBody.innerHTML = `
                <tr><td colspan="6" class="text-center py-6 text-danger">Failed to load customers from backend.</td></tr>
            `;
        }
    }

    // =========================================================================
    // View Navigation
    // =========================================================================
    function switchView(viewName, paramCustomerId = null) {
        state.currentView = viewName;

        // Nav active state
        [elements.navCustomers, elements.navPurchase, elements.navDetails].forEach(btn => btn.classList.remove('active'));
        [elements.viewCustomers, elements.viewPurchase, elements.viewDetails].forEach(sec => sec.classList.remove('active'));

        if (viewName === 'customers') {
            elements.navCustomers.classList.add('active');
            elements.viewCustomers.classList.add('active');
            elements.pageTitle.textContent = 'Customers';
            elements.pageSubtitle.textContent = 'Manage retail loyalty members and points balances';
            elements.btnOpenAddCustomer.classList.remove('hidden');
        } else if (viewName === 'purchase') {
            elements.navPurchase.classList.add('active');
            elements.viewPurchase.classList.add('active');
            elements.pageTitle.textContent = 'New Purchase';
            elements.pageSubtitle.textContent = 'Calculate tier discounts and redeem customer loyalty points';
            elements.btnOpenAddCustomer.classList.add('hidden');

            if (paramCustomerId) {
                elements.purchaseCustomerSelect.value = paramCustomerId;
            }
            onPurchaseInputChange();
        } else if (viewName === 'details') {
            elements.navDetails.classList.add('active');
            elements.viewDetails.classList.add('active');
            elements.pageTitle.textContent = 'Customer Details';
            elements.pageSubtitle.textContent = 'Comprehensive tier progress, lifetime balances, and points ledger';
            elements.btnOpenAddCustomer.classList.add('hidden');

            if (paramCustomerId) {
                state.selectedCustomerId = paramCustomerId;
                elements.detailsCustomerSelect.value = paramCustomerId;
            }
            if (state.selectedCustomerId) {
                elements.detailsCustomerSelect.value = state.selectedCustomerId;
                loadCustomerDetails(state.selectedCustomerId);
            }
        }
    }

    // =========================================================================
    // Customer Table & Search
    // =========================================================================
    function renderCustomersTable() {
        const query = state.searchQuery.toLowerCase().trim();
        const tierFilter = state.activeTierFilter;

        const filtered = state.customers.filter(c => {
            const matchesQuery = !query ||
                c.name.toLowerCase().includes(query) ||
                c.email.toLowerCase().includes(query) ||
                (c.tier && c.tier.name.toLowerCase().includes(query));

            const matchesTier = (tierFilter === 'ALL') || (c.tier && c.tier.name.toUpperCase() === tierFilter.toUpperCase());

            return matchesQuery && matchesTier;
        });

        if (filtered.length === 0) {
            elements.customersTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-8 text-muted">
                        No customers match your criteria. Try adjusting your search or add a new customer.
                    </td>
                </tr>
            `;
            return;
        }

        elements.customersTableBody.innerHTML = filtered.map(c => `
            <tr>
                <td>
                    <div class="customer-cell-name">${escapeHtml(c.name)}</div>
                    <div class="customer-cell-email">#${c.id} &bull; Joined ${formatDate(c.createdAt)}</div>
                </td>
                <td>
                    <div>${escapeHtml(c.email)}</div>
                    <div class="customer-cell-email">${c.phone ? escapeHtml(c.phone) : '—'}</div>
                </td>
                <td>
                    ${renderTierBadge(c.tier ? c.tier.name : 'Base')}
                </td>
                <td class="text-right">
                    <strong class="text-primary">${Number(c.availablePoints).toLocaleString()}</strong>
                </td>
                <td class="text-right">
                    <span>${Number(c.lifetimePoints).toLocaleString()}</span>
                </td>
                <td class="text-center">
                    <div class="action-buttons-cell">
                        <button class="btn btn-secondary btn-sm" onclick="window.LoyaltyApp.startPurchaseFor(${c.id})">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="9" cy="21" r="1"></circle>
                                <circle cx="20" cy="21" r="1"></circle>
                                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                            </svg>
                            <span>Purchase</span>
                        </button>
                        <button class="btn-text-action" onclick="window.LoyaltyApp.viewCustomerProfile(${c.id})">
                            View Profile &rarr;
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    function renderTierBadge(tierName) {
        const name = (tierName || 'Base').toUpperCase();
        let cssClass = 'tier-base';
        if (name === 'SILVER') cssClass = 'tier-silver';
        else if (name === 'GOLD') cssClass = 'tier-gold';
        else if (name === 'PLATINUM') cssClass = 'tier-platinum';

        return `<span class="tier-badge ${cssClass}">${escapeHtml(tierName || 'Base')}</span>`;
    }

    function populateCustomerDropdowns(preferredId = null) {
        const optionsHtml = [
            '<option value="">-- Choose Customer --</option>',
            ...state.customers.map(c => `
                <option value="${c.id}">
                    ${escapeHtml(c.name)} (${c.tier ? c.tier.name : 'Base'} - ${c.availablePoints} pts available)
                </option>
            `)
        ].join('');

        elements.purchaseCustomerSelect.innerHTML = optionsHtml;
        elements.detailsCustomerSelect.innerHTML = optionsHtml;

        if (preferredId) {
            elements.purchaseCustomerSelect.value = preferredId;
            elements.detailsCustomerSelect.value = preferredId;
        }
    }

    // =========================================================================
    // Add Customer Modal
    // =========================================================================
    function openAddCustomerModal() {
        elements.addCustomerForm.reset();
        clearModalErrors();
        elements.modalAddCustomer.classList.remove('hidden');
        elements.custNameInput.focus();
    }

    function closeAddCustomerModal() {
        elements.modalAddCustomer.classList.add('hidden');
        clearModalErrors();
    }

    function clearModalErrors() {
        elements.errorName.textContent = '';
        elements.errorName.classList.add('hidden');
        elements.errorEmail.textContent = '';
        elements.errorEmail.classList.add('hidden');
        elements.errorPhone.textContent = '';
        elements.errorPhone.classList.add('hidden');
        elements.modalGeneralError.textContent = '';
        elements.modalGeneralError.classList.add('hidden');
    }

    async function handleAddCustomerSubmit(e) {
        e.preventDefault();
        clearModalErrors();

        const name = elements.custNameInput.value.trim();
        const email = elements.custEmailInput.value.trim();
        const phone = elements.custPhoneInput.value.trim();

        let hasClientError = false;
        if (!name) {
            elements.errorName.textContent = 'Full name is required';
            elements.errorName.classList.remove('hidden');
            hasClientError = true;
        }
        if (!email) {
            elements.errorEmail.textContent = 'Email address is required';
            elements.errorEmail.classList.remove('hidden');
            hasClientError = true;
        } else if (!isValidEmail(email)) {
            elements.errorEmail.textContent = 'Please enter a valid email address';
            elements.errorEmail.classList.remove('hidden');
            hasClientError = true;
        }

        if (hasClientError) return;

        // Submit to API
        const btn = elements.addCustomerForm.querySelector('button[type="submit"]');
        setButtonLoading(btn, true);

        try {
            const newCustomer = await apiRequest('/api/customers', {
                method: 'POST',
                body: JSON.stringify({ name, email, phone })
            });

            showToast('success', 'Customer Registered', `${newCustomer.name} has been enrolled in Base tier.`);
            closeAddCustomerModal();
            await loadCustomers(newCustomer.id);
            switchView('customers');
        } catch (err) {
            if (err.status === 409) {
                elements.errorEmail.textContent = err.message || 'Email already registered';
                elements.errorEmail.classList.remove('hidden');
            } else if (err.status === 400 && err.response && err.response.fieldErrors) {
                const errors = err.response.fieldErrors;
                if (errors.name) {
                    elements.errorName.textContent = errors.name;
                    elements.errorName.classList.remove('hidden');
                }
                if (errors.email) {
                    elements.errorEmail.textContent = errors.email;
                    elements.errorEmail.classList.remove('hidden');
                }
                if (errors.phone) {
                    elements.errorPhone.textContent = errors.phone;
                    elements.errorPhone.classList.remove('hidden');
                }
            } else {
                elements.modalGeneralError.textContent = err.message || 'Failed to create customer';
                elements.modalGeneralError.classList.remove('hidden');
            }
        } finally {
            setButtonLoading(btn, false);
        }
    }

    // =========================================================================
    // New Purchase & Live Preview
    // =========================================================================
    function getSelectedCustomerForPurchase() {
        const id = parseInt(elements.purchaseCustomerSelect.value, 10);
        if (!id) return null;
        return state.customers.find(c => c.id === id) || null;
    }

    function onPurchaseInputChange() {
        elements.purchaseFormError.classList.add('hidden');
        const customer = getSelectedCustomerForPurchase();

        if (!customer) {
            elements.purchaseCustomerInfo.classList.add('hidden');
            elements.redeemHint.textContent = 'Select a customer to view available points.';
            resetPreview();
            return;
        }

        // Show customer summary pill
        elements.purchaseCustomerInfo.classList.remove('hidden');
        elements.purchaseCustomerInfo.innerHTML = `
            <span><strong>${escapeHtml(customer.name)}</strong> &bull; Tier: <strong>${customer.tier ? customer.tier.name : 'Base'}</strong> (${customer.tier ? customer.tier.discountPercent : 0}% discount)</span>
            <span class="text-primary font-weight-bold">Available: ${customer.availablePoints} pts</span>
        `;
        elements.redeemHint.textContent = `Available balance: ${customer.availablePoints} points (1 pt = Rs. ${state.config.rupeesPerPoint.toFixed(2)})`;

        // Calculate breakdown
        const gross = parseFloat(elements.purchaseAmountInput.value) || 0;
        let redeemPts = parseInt(elements.purchaseRedeemInput.value, 10) || 0;

        const discountPercent = (customer.tier && customer.tier.discountPercent) ? parseFloat(customer.tier.discountPercent) : 0;
        const tierDiscount = (gross > 0) ? Math.round((gross * (discountPercent / 100)) * 100) / 100 : 0;
        const remainingAfterTierDiscount = Math.max(0, gross - tierDiscount);

        // Max redeemable cannot exceed customer's available points and cannot exceed (gross - tierDiscount)
        const maxRedeemByPayable = Math.floor(remainingAfterTierDiscount / state.config.rupeesPerPoint);
        const maxRedeemAllowed = Math.min(customer.availablePoints, maxRedeemByPayable);

        if (redeemPts > maxRedeemAllowed && maxRedeemAllowed >= 0) {
            // User entered more than allowed
            elements.purchaseRedeemInput.setCustomValidity(`Max allowed is ${maxRedeemAllowed} points.`);
        } else {
            elements.purchaseRedeemInput.setCustomValidity('');
        }

        const redeemValue = Math.min(redeemPts * state.config.rupeesPerPoint, remainingAfterTierDiscount);
        const netAmount = Math.max(0, remainingAfterTierDiscount - redeemValue);
        const pointsEarned = Math.floor(netAmount * state.config.pointsPerRupee);

        const projectedAvailable = Math.max(0, customer.availablePoints - redeemPts) + pointsEarned;
        const projectedLifetime = customer.lifetimePoints + pointsEarned;

        // Update preview panel
        elements.previewGross.textContent = `Rs. ${gross.toFixed(2)}`;
        elements.previewTierName.textContent = customer.tier ? customer.tier.name : 'Base';
        elements.previewTierRate.textContent = `${discountPercent}%`;
        elements.previewTierDiscount.textContent = `- Rs. ${tierDiscount.toFixed(2)}`;
        elements.previewRedeemPts.textContent = redeemPts;
        elements.previewRedeemDiscount.textContent = `- Rs. ${redeemValue.toFixed(2)}`;
        elements.previewNet.textContent = `Rs. ${netAmount.toFixed(2)}`;
        elements.previewPointsEarned.textContent = `+${pointsEarned} pts`;
        elements.previewNewBalance.textContent = `${projectedAvailable} pts`;

        // Check projected tier upgrade
        updateTierProjectionNotice(customer, projectedLifetime);
    }

    function resetPreview() {
        elements.previewGross.textContent = 'Rs. 0.00';
        elements.previewTierName.textContent = 'Base';
        elements.previewTierRate.textContent = '0%';
        elements.previewTierDiscount.textContent = '- Rs. 0.00';
        elements.previewRedeemPts.textContent = '0';
        elements.previewRedeemDiscount.textContent = '- Rs. 0.00';
        elements.previewNet.textContent = 'Rs. 0.00';
        elements.previewPointsEarned.textContent = '+0 pts';
        elements.previewNewBalance.textContent = '0 pts';
        elements.previewTierProjection.className = 'tier-projection-banner';
        elements.previewTierProjection.textContent = 'Select a customer and enter bill amount to preview rewards';
    }

    function updateTierProjectionNotice(customer, projectedLifetime) {
        if (!state.tiers || state.tiers.length === 0) return;

        const currentTierMin = customer.tier ? customer.tier.minLifetimePoints : 0;
        let projectedTier = customer.tier;

        for (const t of state.tiers) {
            if (projectedLifetime >= t.minLifetimePoints) {
                if (!projectedTier || t.minLifetimePoints > projectedTier.minLifetimePoints) {
                    projectedTier = t;
                }
            }
        }

        if (projectedTier && projectedTier.minLifetimePoints > currentTierMin) {
            elements.previewTierProjection.className = 'tier-projection-banner upgrade-alert';
            elements.previewTierProjection.innerHTML = `
                🚀 <strong>Upgrade Alert!</strong> This purchase will upgrade <strong>${escapeHtml(customer.name)}</strong> to 
                <strong>${projectedTier.name} Tier</strong> (${projectedTier.discountPercent}% discount)!
            `;
        } else {
            elements.previewTierProjection.className = 'tier-projection-banner';
            elements.previewTierProjection.textContent = `Maintains ${customer.tier ? customer.tier.name : 'Base'} tier. Lifetime points will reach ${projectedLifetime}.`;
        }
    }

    function useMaxRedeemPoints() {
        const customer = getSelectedCustomerForPurchase();
        if (!customer) {
            showToast('error', 'Select Customer', 'Please choose a customer first.');
            return;
        }

        const gross = parseFloat(elements.purchaseAmountInput.value) || 0;
        const discountPercent = (customer.tier && customer.tier.discountPercent) ? parseFloat(customer.tier.discountPercent) : 0;
        const tierDiscount = (gross > 0) ? Math.round((gross * (discountPercent / 100)) * 100) / 100 : 0;
        const remaining = Math.max(0, gross - tierDiscount);

        const maxPointsPayable = Math.floor(remaining / state.config.rupeesPerPoint);
        const maxRedeem = Math.min(customer.availablePoints, maxPointsPayable);

        elements.purchaseRedeemInput.value = Math.max(0, maxRedeem);
        onPurchaseInputChange();
    }

    async function handlePurchaseSubmit(e) {
        e.preventDefault();
        elements.purchaseFormError.classList.add('hidden');

        const customer = getSelectedCustomerForPurchase();
        if (!customer) {
            showFormError('Please select a customer.');
            return;
        }

        const gross = parseFloat(elements.purchaseAmountInput.value);
        if (isNaN(gross) || gross <= 0) {
            showFormError('Please enter a valid gross bill amount greater than 0.');
            return;
        }

        const redeemPts = parseInt(elements.purchaseRedeemInput.value, 10) || 0;
        if (redeemPts < 0) {
            showFormError('Redeem points cannot be negative.');
            return;
        }

        if (redeemPts > customer.availablePoints) {
            showFormError(`Customer only has ${customer.availablePoints} points available.`);
            return;
        }

        setButtonLoading(elements.btnSubmitPurchase, true);

        try {
            const response = await apiRequest(`/api/customers/${customer.id}/purchases`, {
                method: 'POST',
                body: JSON.stringify({
                    amount: gross,
                    redeemPoints: redeemPts
                })
            });

            // Success feedback
            showToast('success', 'Purchase Complete',
                `Paid Rs. ${Number(response.netAmount).toFixed(2)} | Earned +${response.pointsEarned} points!`);

            if (response.upgraded) {
                showCelebrationBanner(response.customerName, response.oldTier, response.newTier);
            }

            // Refresh customers data and navigate
            await loadCustomers(customer.id);
            elements.purchaseAmountInput.value = '';
            elements.purchaseRedeemInput.value = '0';
            onPurchaseInputChange();

            // Switch to Customer Details to show updated ledger!
            switchView('details', customer.id);
        } catch (err) {
            showFormError(err.message || 'Failed to process purchase');
            showToast('error', 'Purchase Failed', err.message || 'Validation error');
        } finally {
            setButtonLoading(elements.btnSubmitPurchase, false);
        }
    }

    function showFormError(msg) {
        elements.purchaseFormError.textContent = msg;
        elements.purchaseFormError.classList.remove('hidden');
    }

    // =========================================================================
    // Celebratory Upgrade Banner
    // =========================================================================
    function showCelebrationBanner(customerName, oldTier, newTier) {
        elements.celebrationBannerContainer.innerHTML = `
            <div class="celebration-card">
                <div class="celebration-content">
                    <h3>🎉 Congratulations! Upgraded to ${escapeHtml(newTier)}!</h3>
                    <p>
                        <strong>${escapeHtml(customerName)}</strong> has ascended from <strong>${escapeHtml(oldTier)}</strong> to 
                        <strong>${escapeHtml(newTier)} Tier</strong>! Enjoy exclusive higher discount perks on all upcoming retail purchases.
                    </p>
                </div>
                <button class="celebration-close" onclick="document.getElementById('celebration-banner-container').classList.add('hidden')" aria-label="Dismiss banner">&times;</button>
            </div>
        `;
        elements.celebrationBannerContainer.classList.remove('hidden');

        // Auto dismiss after 12 seconds
        setTimeout(() => {
            elements.celebrationBannerContainer.classList.add('hidden');
        }, 12000);
    }

    // =========================================================================
    // Customer Details & Points History
    // =========================================================================
    async function loadCustomerDetails(customerId) {
        if (!customerId) return;
        state.selectedCustomerId = customerId;

        try {
            const [customer, summary] = await Promise.all([
                apiRequest(`/api/customers/${customerId}`),
                apiRequest(`/api/customers/${customerId}/summary`)
            ]);

            renderProfileOverview(customer, summary);
            await loadPointsHistory(customerId, 0);
        } catch (err) {
            showToast('error', 'Error', 'Failed to load customer profile');
        }
    }

    function renderProfileOverview(customer, summary) {
        elements.detailsName.textContent = customer.name;
        elements.detailsEmail.textContent = customer.email;
        elements.detailsPhone.textContent = customer.phone || 'No phone provided';
        elements.detailsAvatar.textContent = getInitials(customer.name);

        elements.detailsTierBadge.innerHTML = renderTierBadge(summary.tier);
        elements.detailsAvailablePts.textContent = Number(summary.availablePoints).toLocaleString();
        elements.detailsLifetimePts.textContent = Number(summary.lifetimePoints).toLocaleString();
        elements.detailsDiscountPct.textContent = `${summary.discountPercent}%`;

        // Progress to next tier
        if (summary.nextTierName && summary.pointsToNextTier > 0) {
            elements.detailsNextTierLabel.textContent = `${summary.pointsToNextTier} points to ${summary.nextTierName}`;

            // Calculate progress percentage
            let currentTierMin = 0;
            let nextTierMin = 5000;
            const currentTierObj = state.tiers.find(t => t.name === summary.tier);
            const nextTierObj = state.tiers.find(t => t.name === summary.nextTierName);

            if (currentTierObj) currentTierMin = currentTierObj.minLifetimePoints;
            if (nextTierObj) nextTierMin = nextTierObj.minLifetimePoints;

            const span = nextTierMin - currentTierMin;
            const progress = (span > 0) ? Math.min(100, Math.max(0, ((summary.lifetimePoints - currentTierMin) / span) * 100)) : 100;
            elements.detailsProgressFill.style.width = `${progress.toFixed(1)}%`;
        } else {
            elements.detailsNextTierLabel.textContent = 'Maximum Tier Reached (Platinum)';
            elements.detailsProgressFill.style.width = '100%';
        }
    }

    async function loadPointsHistory(customerId, page) {
        state.historyPage = page;
        try {
            const data = await apiRequest(`/api/customers/${customerId}/points-history?page=${page}&size=${state.historyPageSize}&sort=createdAt,desc`);
            renderPointsHistoryTable(data);
        } catch (err) {
            console.error('Failed to load points history', err);
        }
    }

    function renderPointsHistoryTable(pageData) {
        const items = pageData.content || [];
        state.historyTotalPages = pageData.totalPages || 1;

        elements.detailsHistoryTotal.textContent = `${pageData.totalElements || 0} Transactions`;
        elements.historyPageInfo.textContent = `Page ${state.historyPage + 1} of ${Math.max(1, state.historyTotalPages)}`;

        elements.btnHistoryPrev.disabled = state.historyPage <= 0;
        elements.btnHistoryNext.disabled = state.historyPage >= state.historyTotalPages - 1;

        if (items.length === 0) {
            elements.historyTableBody.innerHTML = `
                <tr><td colspan="5" class="text-center py-6 text-muted">No points ledger records found.</td></tr>
            `;
            return;
        }

        elements.historyTableBody.innerHTML = items.map(entry => {
            const isEarn = entry.type === 'EARN';
            const badgeClass = isEarn ? 'type-earn' : 'type-redeem';
            const ptsSign = isEarn ? '+' : '-';
            const ptsColor = isEarn ? 'text-success' : 'text-danger';

            return `
                <tr>
                    <td>${formatDateTime(entry.createdAt)}</td>
                    <td><span class="type-badge ${badgeClass}">${entry.type}</span></td>
                    <td class="${ptsColor} font-weight-bold">${ptsSign}${entry.points} pts</td>
                    <td><strong>${entry.balanceAfter} pts</strong></td>
                    <td class="text-muted">${escapeHtml(entry.description || '—')}</td>
                </tr>
            `;
        }).join('');
    }

    // =========================================================================
    // Toast Notifications
    // =========================================================================
    function showToast(type, title, message) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const iconSvg = (type === 'success')
            ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>'
            : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>';

        toast.innerHTML = `
            <div class="toast-icon">${iconSvg}</div>
            <div class="toast-content">
                <div class="toast-title">${escapeHtml(title)}</div>
                <div class="toast-message">${escapeHtml(message)}</div>
            </div>
            <button class="toast-close" aria-label="Close notification">&times;</button>
        `;

        toast.querySelector('.toast-close').addEventListener('click', () => {
            toast.remove();
        });

        elements.toastContainer.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                toast.style.opacity = '0';
                setTimeout(() => toast.remove(), 200);
            }
        }, 5000);
    }

    // =========================================================================
    // Utilities
    // =========================================================================
    function setButtonLoading(button, isLoading) {
        if (!button) return;
        const textSpan = button.querySelector('.btn-text');
        const loaderSpan = button.querySelector('.btn-loader');
        if (isLoading) {
            button.disabled = true;
            if (textSpan) textSpan.classList.add('hidden');
            if (loaderSpan) loaderSpan.classList.remove('hidden');
        } else {
            button.disabled = false;
            if (textSpan) textSpan.classList.remove('hidden');
            if (loaderSpan) loaderSpan.classList.add('hidden');
        }
    }

    function getInitials(name) {
        if (!name) return 'LP';
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatDate(dateStr) {
        if (!dateStr) return '—';
        const d = new Date(dateStr);
        return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
    }

    function formatDateTime(dateStr) {
        if (!dateStr) return '—';
        const d = new Date(dateStr);
        return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }

    // =========================================================================
    // Event Listeners
    // =========================================================================
    function setupEventListeners() {
        // Nav items
        elements.navCustomers.addEventListener('click', () => switchView('customers'));
        elements.navPurchase.addEventListener('click', () => switchView('purchase'));
        elements.navDetails.addEventListener('click', () => switchView('details'));

        // Refresh button
        elements.btnRefresh.addEventListener('click', async () => {
            showToast('info', 'Refreshing', 'Updating data from engine...');
            await loadCustomers();
            if (state.currentView === 'details' && state.selectedCustomerId) {
                await loadCustomerDetails(state.selectedCustomerId);
            }
        });

        // Add Customer Modal
        elements.btnOpenAddCustomer.addEventListener('click', openAddCustomerModal);
        elements.btnCloseModal.addEventListener('click', closeAddCustomerModal);
        elements.btnCancelModal.addEventListener('click', closeAddCustomerModal);
        elements.addCustomerForm.addEventListener('submit', handleAddCustomerSubmit);

        // Search & Filter
        elements.customerSearchInput.addEventListener('input', (e) => {
            state.searchQuery = e.target.value;
            renderCustomersTable();
        });

        elements.tierFilterPills.addEventListener('click', (e) => {
            const pill = e.target.closest('.filter-pill');
            if (!pill) return;
            elements.tierFilterPills.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            state.activeTierFilter = pill.dataset.tier;
            renderCustomersTable();
        });

        // Purchase view inputs
        elements.purchaseCustomerSelect.addEventListener('change', onPurchaseInputChange);
        elements.purchaseAmountInput.addEventListener('input', onPurchaseInputChange);
        elements.purchaseRedeemInput.addEventListener('input', onPurchaseInputChange);
        elements.btnUseMaxPoints.addEventListener('click', useMaxRedeemPoints);
        elements.purchaseForm.addEventListener('submit', handlePurchaseSubmit);

        // Quick amount chips
        document.querySelectorAll('.chip-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                elements.purchaseAmountInput.value = btn.dataset.amount;
                onPurchaseInputChange();
            });
        });

        // Customer details selector
        elements.detailsCustomerSelect.addEventListener('change', (e) => {
            const id = parseInt(e.target.value, 10);
            if (id) {
                loadCustomerDetails(id);
            }
        });

        elements.btnQuickPurchaseFromDetails.addEventListener('click', () => {
            if (state.selectedCustomerId) {
                switchView('purchase', state.selectedCustomerId);
            }
        });

        // Pagination buttons
        elements.btnHistoryPrev.addEventListener('click', () => {
            if (state.historyPage > 0) {
                loadPointsHistory(state.selectedCustomerId, state.historyPage - 1);
            }
        });

        elements.btnHistoryNext.addEventListener('click', () => {
            if (state.historyPage < state.historyTotalPages - 1) {
                loadPointsHistory(state.selectedCustomerId, state.historyPage + 1);
            }
        });
    }

    // Expose global actions for inline row button handlers
    window.LoyaltyApp = {
        startPurchaseFor: function (customerId) {
            switchView('purchase', customerId);
        },
        viewCustomerProfile: function (customerId) {
            switchView('details', customerId);
        }
    };

    // Boot
    document.addEventListener('DOMContentLoaded', init);
})();
