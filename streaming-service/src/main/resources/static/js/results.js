/**
 * Global state for packages and selected packages
 * packages: Stores all available packages from initial search
 * selectedPackages: Tracks user-selected packages for comparison
 */
let packages = [];
let selectedPackages = new Set();

$(document).ready(function() {
    const selectedTeams = JSON.parse(sessionStorage.getItem('selectedTeams') || '[]');
    const selectedTournaments = JSON.parse(sessionStorage.getItem('selectedTournaments') || '[]');
    
    console.log('Selected teams:', selectedTeams);
    console.log('Selected tournaments:', selectedTournaments);
    
    if (!selectedTeams.length && !selectedTournaments.length) {
        window.location.href = 'index.html';
        return;
    }

    // Initial search with teams and tournaments
    searchPackages(selectedTeams, selectedTournaments);
    setupFilterListeners();
});

function searchPackages(teams, tournaments) {
    console.log('Searching packages for teams:', teams, 'tournaments:', tournaments);
    
    fetch('/api/search', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            teams: teams,
            tournaments: tournaments
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                console.error('Error response:', text);
                throw new Error(text);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Received packages:', data);
        packages = data; // Store initial results
        displayPackages(data);
        // Show filter section after initial results load
        document.querySelector('.filter-section').style.display = 'block';
    })
    .catch(error => {
        console.error('Error:', error);
        const container = document.getElementById('packageResults');
        container.innerHTML = `<p style="color: red;">Error loading packages: ${error.message}</p>`;
    });
}

/**
 * Filter state object to track current filter settings
 */
let filterState = {
    sortingOption: null,
    preference: null,
    maxPrice: null
};

function updateFilterState(event) {
    const field = event.target.id;
    switch(field) {
        case 'coverage':
            filterState.preference = event.target.value || null;
            break;
        case 'sortBy':
            filterState.sortingOption = event.target.value || null;
            break;
        case 'maxPrice':
            filterState.maxPrice = event.target.value ? 
                parseFloat(event.target.value) : null;
            break;
    }
}


/**
 * Initializes event listeners for filter controls.
 * Sets up handlers for:
 * - Price input
 * - Coverage preference
 * - Sort options
 * - Filter application/clearing
 */
function setupFilterListeners() {
    document.getElementById('maxPrice')?.addEventListener('input', updateFilterState);
    document.getElementById('coverage')?.addEventListener('change', updateFilterState);
    document.getElementById('sortBy')?.addEventListener('change', updateFilterState);

        // Add form submit handler
        const filterForm = document.querySelector('.filter-section form');
        filterForm?.addEventListener('submit', (e) => {
            e.preventDefault(); // Prevent form submission
        });
    
        // Add button click handlers
        document.querySelector('.apply-filters-btn')?.addEventListener('click', (e) => {
            e.preventDefault();
            applyFilters();
        });
    
        document.querySelector('.clear-filters-btn')?.addEventListener('click', (e) => {
            e.preventDefault();
            clearFilters();
        });

}

function applyFilters() {
    if (!packages || packages.length === 0) {
        console.error('No packages available to filter');
        return;
    }

  
    // Send both the current packages and filter options
    const requestBody = {
        packages: packages,  
        filterOptions: {
            sortingOption: filterState.sortingOption,
            preference: filterState.preference,
            maxPrice: filterState.maxPrice
        }
    };

    fetch('/api/filter', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        displayPackages(data);
    })
    .catch(error => {
        console.error('Error applying filters:', error);
        alert('Error applying filters. Please try again.');
    });
}

function clearFilters() {
    document.getElementById('coverage').value = '';
    document.getElementById('maxPrice').value = '';
    document.getElementById('sortBy').value = '';
    
    filterState = {
        sortingOption: null,
        preference: null,      
        maxPrice: null
    };

    // Display the original packages
    displayPackages(packages);
}

function displayPackages(receivedPackages) {
    const packageResults = document.getElementById('packageResults');
    packageResults.innerHTML = '';

    if (!receivedPackages || receivedPackages.length === 0) {
        packageResults.innerHTML = '<p>No packages found matching your criteria.</p>';
        return;
    }

    receivedPackages.forEach(pkg => {
        const card = document.createElement('div');
        card.className = 'package-card';
        const monthlyPrice = (pkg.monthlyPriceCents / 100).toFixed(2);
        card.innerHTML = `
            <h3>${pkg.name}</h3>
            <p>Monthly Price: €${(monthlyPrice)}</p>
            <p>Live Coverage: ${(pkg.liveCoveragePercentage * 100).toFixed(1)}%</p>
            <p>Highlights Coverage: ${(pkg.highlightsCoveragePercentage * 100).toFixed(1)}%</p>
        `;
        packageResults.appendChild(card);
    });
}


function findBestCombination() {
    const selectedTeams = JSON.parse(sessionStorage.getItem('selectedTeams') || '[]');
    const selectedTournaments = JSON.parse(sessionStorage.getItem('selectedTournaments') || '[]');
    const packageDTOs = packages.map(pkg => ({
        streamingPackageId: pkg.streamingPackageId,
        name: pkg.name,
        monthlyPriceCents: pkg.monthlyPriceCents,
        liveCoveragePercentage: pkg.liveCoveragePercentage,
        highlightsCoveragePercentage: pkg.highlightsCoveragePercentage
    }));


    console.log('Original packages:', packages); // Log original packages

    // Check if packages exists and has data
    if (!packages || packages.length === 0) {
        console.error('No packages available');
        alert('No packages available for comparison');
        return;
    }
    console.log('Package DTOs:', packageDTOs);

    fetch('api/best-combination', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            teams: selectedTeams,
            tournaments : selectedTournaments,
            packages: packageDTOs
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(bestCombination => {
        sessionStorage.setItem('bestCombination', JSON.stringify(bestCombination));
        window.location.href = 'comparison.html';
    }).catch(error => {
        console.error('Error finding best combination:', error);
        alert('Error finding best combination. Please try again.');
    });   
}
