// Frontend API Integration and State Management

let currentPage = 0;
let totalPages = 1;
const pageSize = 12; // 12 jobs per grid page
let searchTimeout = null;

// Initialize
document.addEventListener("DOMContentLoaded", () => {
    checkHealth();
    fetchJobs();
});

// 1. Health Status Check
async function checkHealth() {
    const healthStatusEl = document.getElementById("healthStatus");
    try {
        const res = await fetch("/api/health");
        if (res.ok) {
            const data = await res.json();
            if (data.status === "UP") {
                healthStatusEl.innerHTML = `
                    <span class="status-indicator online"></span>
                    <span class="status-label">Server Connected</span>
                `;
            }
        } else {
            throw new Error();
        }
    } catch (e) {
        healthStatusEl.innerHTML = `
            <span class="status-indicator offline"></span>
            <span class="status-label">Server Offline</span>
        `;
    }
}

// 2. Fetch Job Listings
async function fetchJobs() {
    const keyword = document.getElementById("inputKeyword").value;
    const location = document.getElementById("inputLocation").value;
    
    // Toggle UI States
    showState("loadingState");
    hideElement("jobsGrid");
    hideElement("paginationPanel");

    try {
        // Construct query parameters
        let url = `/api/jobs?page=${currentPage}&size=${pageSize}`;
        if (keyword.trim()) {
            url += `&keyword=${encodeURIComponent(keyword.trim())}`;
        }
        if (location.trim()) {
            url += `&location=${encodeURIComponent(location.trim())}`;
        }

        const res = await fetch(url);
        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }
        
        const data = await res.json();
        
        if (!data.content || data.content.length === 0) {
            showState("emptyState");
            return;
        }

        totalPages = data.totalPages;
        renderJobs(data.content);
        renderPagination();
        showState(null); // Clear loading state
        showElement("jobsGrid");
        showElement("paginationPanel");

    } catch (e) {
        console.error("Failed to fetch jobs:", e);
        document.getElementById("errorMessage").innerText = e.message || "Failed to communicate with the Spring Boot server.";
        showState("errorState");
    }
}

// 3. Render Job Cards
function renderJobs(jobs) {
    const grid = document.getElementById("jobsGrid");
    grid.innerHTML = ""; // Clear existing

    jobs.forEach(job => {
        const card = document.createElement("div");
        card.className = "job-card glass-card";

        // Create tags html
        const tagsHtml = (job.tags || [])
            .slice(0, 3)
            .map(t => `<span class="tag">${escapeHtml(t)}</span>`)
            .join("");

        // Format date
        const pubDateStr = job.publishedAt ? formatDate(job.publishedAt) : "Recently";

        card.innerHTML = `
            <div class="job-header">
                <div class="job-meta-top">
                    <span class="company-name">${escapeHtml(job.company)}</span>
                    ${job.remote ? '<span class="tag" style="color:#10B981;background:rgba(16,185,129,0.08);border-color:rgba(16,185,129,0.15)">Remote</span>' : ''}
                </div>
                <h2 class="job-title">${escapeHtml(job.title)}</h2>
                <div class="location-badge">
                    <svg style="width:12px;height:12px;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                        <circle cx="12" cy="10" r="3"></circle>
                    </svg>
                    <span>${escapeHtml(job.location)}</span>
                </div>
            </div>
            <div class="job-body">
                <p class="job-desc">${stripHtml(job.description)}</p>
                <div class="tag-list">
                    ${tagsHtml}
                </div>
            </div>
            <div class="job-footer">
                <span class="pub-date">${pubDateStr}</span>
                <a href="${job.url}" target="_blank" class="btn-view-job">View Listing</a>
            </div>
        `;
        grid.appendChild(card);
    });
}

// 4. Manual Ingestion Action
async function triggerIngestion() {
    const btn = document.getElementById("btnIngest");
    const spinner = document.getElementById("ingestSpinner");
    
    // Disable and show spinner
    btn.disabled = true;
    spinner.classList.remove("hidden");

    try {
        const res = await fetch("/api/jobs/ingest", { method: "POST" });
        if (!res.ok) {
            throw new Error(`Ingestion failed with status: ${res.status}`);
        }
        
        const summary = await res.json();
        
        // Populate stats banner
        document.getElementById("statFetched").innerText = summary.fetched;
        document.getElementById("statInserted").innerText = summary.inserted;
        document.getElementById("statUpdated").innerText = summary.updated;
        document.getElementById("statSkipped").innerText = summary.skipped;
        document.getElementById("statFailed").innerText = summary.failed;

        // Show banner
        const banner = document.getElementById("statsBanner");
        banner.classList.remove("hidden");
        
        // Refresh job listings
        currentPage = 0;
        fetchJobs();

    } catch (e) {
        alert("Error during manual ingestion. See console or server logs for details.");
        console.error(e);
    } finally {
        btn.disabled = false;
        spinner.classList.add("hidden");
    }
}

// Helpers
function handleSearch() {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        currentPage = 0;
        fetchJobs();
    }, 400); // Debounce input searches by 400ms
}

function changePage(direction) {
    const targetPage = currentPage + direction;
    if (targetPage >= 0 && targetPage < totalPages) {
        currentPage = targetPage;
        fetchJobs();
    }
}

function renderPagination() {
    document.getElementById("btnPrev").disabled = (currentPage === 0);
    document.getElementById("btnNext").disabled = (currentPage >= totalPages - 1);
    document.getElementById("pageIndicator").innerText = `Page ${currentPage + 1} of ${Math.max(1, totalPages)}`;
}

function showState(stateId) {
    const states = ["loadingState", "errorState", "emptyState"];
    states.forEach(s => {
        const el = document.getElementById(s);
        if (s === stateId) {
            el.classList.remove("hidden");
        } else {
            el.classList.add("hidden");
        }
    });
}

function hideStats() {
    document.getElementById("statsBanner").classList.add("hidden");
}

function showElement(id) { document.getElementById(id).classList.remove("hidden"); }
function hideElement(id) { document.getElementById(id).classList.add("hidden"); }

function formatDate(isoString) {
    try {
        const date = new Date(isoString);
        return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
        return "Recently";
    }
}

function stripHtml(html) {
    if (!html) return "";
    let doc = new DOMParser().parseFromString(html, 'text/html');
    return doc.body.textContent || "";
}

function escapeHtml(text) {
    if (!text) return "";
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
