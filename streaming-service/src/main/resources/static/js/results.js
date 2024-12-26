let packages = [];
let selectedPackages = new Set();

$(document).ready(function() {
    const selectedTeams = JSON.parse(sessionStorage.getItem('selectedTeams') || '[]');
    const selectedTournaments = JSON.parse(sessionStorage.getItem('selectedTournaments') || '[]');
    
    if (!selectedTeams.length && !selectedTournaments.length) {
        window.location.href = '/index.html';
        return;
    }

    loadPackages({
        teams: selectedTeams,
        tournaments: selectedTournaments,
        preference: 'NONE',
        sortingOption: 'PRICE'
    });
});

function loadPackages(filterOptions) {
    fetch('/api/filter', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(filterOptions)
    })
    .then(response => response.json())
    .then(displayPackages);
}

function displayPackages(receivedPackages) {
    packages = receivedPackages;
    const container = document.getElementById('packageResults');
    container.innerHTML = '';

    packages.forEach(pkg => {
        const card = document.createElement('div');
        card.className = 'package-card';
        card.innerHTML = `
            <h3>${pkg.name}</h3>
            <p>Monthly Price: €${(pkg.monthlyPrice / 100).toFixed(2)}</p>
            <p>Coverage: ${(pkg.coverage * 100).toFixed(1)}%</p>
            <input type="checkbox" onchange="togglePackageSelection(${pkg.streamingPackageId})" 
                   ${selectedPackages.has(pkg.streamingPackageId) ? 'checked' : ''}>
        `;
        container.appendChild(card);
    });
}

function togglePackageSelection(packageId) {
    if (selectedPackages.has(packageId)) {
        selectedPackages.delete(packageId);
    } else if (selectedPackages.size < 3) {
        selectedPackages.add(packageId);
    } else {
        alert('You can only select up to 3 packages');
        event.target.checked = false;
        return;
    }

    document.getElementById('compareButton').disabled = selectedPackages.size < 1;
}

function applyFilters() {
    const filterOptions = {
        teams: JSON.parse(sessionStorage.getItem('selectedTeams')),
        tournaments: JSON.parse(sessionStorage.getItem('selectedTournaments')),
        preference: document.querySelector('input[name="preference"]:checked')?.value || 'NONE',
        sortingOption: document.getElementById('sortBy').value,
        maxPrice: document.getElementById('maxPrice').value || null
    };

    loadPackages(filterOptions);
}

function compareSelected() {
    const filterOptions = {
        teams: JSON.parse(sessionStorage.getItem('selectedTeams')),
        tournaments: JSON.parse(sessionStorage.getItem('selectedTournaments')),
        preference: document.querySelector('input[name="preference"]:checked')?.value || 'NONE'
    };

    fetch('/api/compare', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            packageIds: Array.from(selectedPackages),
            options: filterOptions
        })
    })
    .then(response => response.json())
    .then(displayComparison);
}

function displayComparison(combination) {
    const comparisonSection = document.getElementById('packageComparison');
    comparisonSection.style.display = 'block';
    document.getElementById('bestCombination').style.display = 'none';

    comparisonSection.innerHTML = `
        <h2>Optimal Package Combination</h2>
        <div class="combination-result">
            <h3>Selected Packages:</h3>
            <ul>
                ${combination.packages.map(pkg => `
                    <li>${pkg.name} - €${(pkg.monthlyPrice / 100).toFixed(2)}/month</li>
                `).join('')}
            </ul>
            
            <div class="coverage-details">
                <p>Live Coverage: ${(combination.totalLiveCoverage * 100).toFixed(1)}%</p>
                <p>Highlights Coverage: ${(combination.totalHighlightCoverage * 100).toFixed(1)}%</p>
                <p>Total Monthly Price: €${(combination.totalPrice / 100).toFixed(2)}</p>
            </div>
        </div>
    `;
}