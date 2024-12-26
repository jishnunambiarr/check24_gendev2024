$(document).ready(function() {
    // Initialize Select2
    $('.select2').select2({
        width: '100%',
        placeholder: 'Select options...'
    });
    
    fetchTeams();
    fetchTournaments();
});

function fetchTeams() {
    fetch('/api/teams')
        .then(response => response.json())
        .then(teams => {
            const teamSelect = document.getElementById('teamSelect');
            teams.forEach(team => {
                const option = new Option(team, team);
                teamSelect.appendChild(option);
            });
        });
}

function fetchTournaments() {
    fetch('/api/tournaments')
        .then(response => response.json())
        .then(tournaments => {
            const tournamentSelect = document.getElementById('tournamentSelect');
            tournaments.forEach(tournament => {
                const option = new Option(tournament, tournament);
                tournamentSelect.appendChild(option);
            });
        });
}

function findPackages() {
    const selectedTeams = $('#teamSelect').val();
    const selectedTournaments = $('#tournamentSelect').val();

    if (!selectedTeams.length && !selectedTournaments.length) {
        alert('Please select at least one team or tournament');
        return;
    }

    // Store selections in sessionStorage for results page
    sessionStorage.setItem('selectedTeams', JSON.stringify(selectedTeams));
    sessionStorage.setItem('selectedTournaments', JSON.stringify(selectedTournaments));
    
    window.location.href = '/results.html';
}