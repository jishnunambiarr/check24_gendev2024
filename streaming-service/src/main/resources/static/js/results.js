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

function setupFilterListeners() {
    document.getElementById('maxPrice')?.addEventListener('change', applyFilters);
    document.getElementById('coveragePreference')?.addEventListener('change', applyFilters);
    document.getElementById('sortOption')?.addEventListener('change', applyFilters);
}

function applyFilters() {
    const maxPrice = document.getElementById('maxPrice')?.value;
    const preference = document.getElementById('coveragePreference')?.value;
    const sortOption = document.getElementById('sortOption')?.value;

    const filterOptions = {
        maxPrice: maxPrice ? parseFloat(maxPrice) : null,
        preference: preference || null,
        sortingOption: sortOption || null
    };

    const packages = JSON.parse(sessionStorage.getItem('packages') || '[]');
    
    displayPackages(packages, filterOptions);
}


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

function applyFilters() {
    const filterOptions = {
        preference: document.querySelector('input[name="preference"]:checked')?.value || null,
        sortingOption: document.getElementById('sortBy').value || null,
        maxPrice: document.getElementById('maxPrice').value ? 
                 parseFloat(document.getElementById('maxPrice').value) : null
    };

    // Send both the current packages and filter options
    const requestBody = {
        packages: packages,  // This is our global packages array that was populated during initial search
        filterOptions: filterOptions
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
    document.querySelectorAll('input[name="preference"]').forEach(radio => {
        radio.checked = false;
    });
    document.getElementById('maxPrice').value = '';
    document.getElementById('sortBy').value = '';
    
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
            <input type="checkbox" onchange="togglePackageSelection(${pkg.streamingPackageId})" 
                   ${selectedPackages.has(pkg.streamingPackageId) ? 'checked' : ''}>
            <label>Select for comparison</label>
        `;
        packageResults.appendChild(card);
    });
}

function findBestCombination() {
    const selectedTeams = JSON.parse(sessionStorage.getItem('selectedTeams') || '[]');
    const selectedTournaments = JSON.parse(sessionStorage.getItem('selectedTournaments') || '[]');

    fetch('api/best-combination', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            teams: selectedTeams,
            tournaments : selectedTournaments,
            packages: packages
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
