/**
 * Müzik Çalar - Professional Android Music Player Web Frontend
 * Native Kotlin Bridge + Media3 + Room Database Integration
 */

(function () {
  'use strict';

  // --- Native Bridge Wrapper ---
  const Bridge = {
    isAvailable: () => typeof window.AndroidMusicBridge !== 'undefined',

    checkPermissions: function () {
      if (this.isAvailable()) return window.AndroidMusicBridge.checkPermissions();
      return true;
    },

    requestPermissions: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.requestPermissions();
      else showToast('İzin isteği (Native modda çalışır)');
    },

    scanLibrary: function () {
      if (this.isAvailable()) return window.AndroidMusicBridge.scanLibrary();
      setTimeout(() => window.onScanCompleted(0), 500);
      return "STARTED";
    },

    getSongs: function (sortOrder) {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getSongs(sortOrder || 'title_asc'));
        } catch (e) {
          console.error('getSongs error:', e);
          return [];
        }
      }
      return [];
    },

    searchSongs: function (query) {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.searchSongs(query));
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    playSong: function (songId, queue, index) {
      if (this.isAvailable()) {
        window.AndroidMusicBridge.playSong(songId, JSON.stringify(queue), index);
      }
    },

    pause: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.pause();
    },

    resume: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.resume();
    },

    next: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.next();
    },

    previous: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.previous();
    },

    seek: function (positionMs) {
      if (this.isAvailable()) window.AndroidMusicBridge.seek(positionMs);
    },

    seekBy: function (deltaMs) {
      if (this.isAvailable()) window.AndroidMusicBridge.seekBy(deltaMs);
    },

    setKeepScreenOn: function (enabled) {
      if (this.isAvailable()) window.AndroidMusicBridge.setKeepScreenOn(enabled);
    },

    loadDemoTracks: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.loadDemoTracks());
        } catch (e) {
          console.error('loadDemoTracks error:', e);
          return [];
        }
      }
      return [];
    },

    toggleShuffle: function () {
      if (this.isAvailable()) return window.AndroidMusicBridge.toggleShuffle();
      return false;
    },

    setRepeatMode: function (mode) {
      if (this.isAvailable()) window.AndroidMusicBridge.setRepeatMode(mode);
    },

    setPlaybackSpeed: function (speed) {
      if (this.isAvailable()) window.AndroidMusicBridge.setPlaybackSpeed(speed);
    },

    addToQueueNext: function (song) {
      if (this.isAvailable()) window.AndroidMusicBridge.addToQueueNext(JSON.stringify(song));
      showToast('Sıradakine eklendi');
    },

    addToQueueEnd: function (song) {
      if (this.isAvailable()) window.AndroidMusicBridge.addToQueueEnd(JSON.stringify(song));
      showToast('Kuyruğun sonuna eklendi');
    },

    removeFromQueue: function (index) {
      if (this.isAvailable()) window.AndroidMusicBridge.removeFromQueue(index);
    },

    clearQueue: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.clearQueue();
    },

    getCurrentState: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getCurrentState());
        } catch (e) {
          return null;
        }
      }
      return null;
    },

    toggleFavorite: function (songId) {
      if (this.isAvailable()) return window.AndroidMusicBridge.toggleFavorite(songId);
      return false;
    },

    getFavorites: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getFavorites());
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getHistory: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getHistory());
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    clearHistory: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.clearHistory();
    },

    getPlaylists: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getPlaylists());
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    createPlaylist: function (name) {
      if (this.isAvailable()) return window.AndroidMusicBridge.createPlaylist(name);
      return 0;
    },

    deletePlaylist: function (id) {
      if (this.isAvailable()) window.AndroidMusicBridge.deletePlaylist(id);
    },

    addSongToPlaylist: function (playlistId, songId) {
      if (this.isAvailable()) window.AndroidMusicBridge.addSongToPlaylist(playlistId, songId);
    },

    removeSongFromPlaylist: function (playlistId, songId) {
      if (this.isAvailable()) window.AndroidMusicBridge.removeSongFromPlaylist(playlistId, songId);
    },

    getPlaylistSongs: function (playlistId) {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getPlaylistSongs(playlistId));
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getFolders: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getFolders());
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getFolderSongs: function (folderPath) {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getFolderSongs(folderPath));
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getArtists: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getArtists());
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getArtistSongs: function (artist) {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getArtistSongs(artist));
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getAlbums: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getAlbums());
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    getAlbumSongs: function (album) {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getAlbumSongs(album));
        } catch (e) {
          return [];
        }
      }
      return [];
    },

    pickFolder: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.pickFolder();
      else showToast('SAF klasör seçimi (Native cihazda çalışır)');
    },

    shareSong: function (songId) {
      if (this.isAvailable()) window.AndroidMusicBridge.shareSong(songId);
    },

    deleteSong: function (songId) {
      if (this.isAvailable()) return window.AndroidMusicBridge.deleteSong(songId);
      return false;
    },

    getEqualizerData: function () {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getEqualizerData());
        } catch (e) {
          return null;
        }
      }
      return null;
    },

    setEqualizerBandLevel: function (band, level) {
      if (this.isAvailable()) window.AndroidMusicBridge.setEqualizerBandLevel(band, level);
    },

    setEqualizerPreset: function (preset) {
      if (this.isAvailable()) window.AndroidMusicBridge.setEqualizerPreset(preset);
    },

    setBassBoost: function (strength) {
      if (this.isAvailable()) window.AndroidMusicBridge.setBassBoost(strength);
    },

    setSleepTimer: function (minutes) {
      if (this.isAvailable()) window.AndroidMusicBridge.setSleepTimer(minutes);
    },

    cancelSleepTimer: function () {
      if (this.isAvailable()) window.AndroidMusicBridge.cancelSleepTimer();
    },

    getSleepTimerRemaining: function () {
      if (this.isAvailable()) return window.AndroidMusicBridge.getSleepTimerRemaining();
      return 0;
    },

    getSetting: function (key, defVal) {
      if (this.isAvailable()) return window.AndroidMusicBridge.getSetting(key, defVal);
      return localStorage.getItem(key) || defVal;
    },

    setSetting: function (key, val) {
      if (this.isAvailable()) window.AndroidMusicBridge.setSetting(key, val);
      try { localStorage.setItem(key, val); } catch (e) {}
    }
  };

  // --- App State ---
  const state = {
    songs: [],
    folders: [],
    playlists: [],
    favorites: [],
    history: [],
    artists: [],
    albums: [],
    currentSong: null,
    isPlaying: false,
    position: 0,
    duration: 0,
    isShuffle: false,
    repeatMode: 0, // 0: OFF, 1: ALL, 2: ONE
    playbackSpeed: 1.0,
    queue: [],
    currentQueueIndex: -1,
    activeScreen: 'screenSongs',
    searchQuery: '',
    sortOrder: 'title_asc',
    isSeeking: false,
    selectedContextSong: null,
    sleepTimerRemaining: 0,
    sleepTimerInterval: null,
    equalizerData: null,
    theme: 'dark',
    neonTheme: 'cyber',
    edgeLightEnabled: true,
    seekStepSeconds: 10,
    ambientBlurEnabled: true,
    keepScreenOn: true,
    spinningVinylEnabled: true
  };

  // --- DOM Elements ---
  const elements = {
    app: document.getElementById('app'),
    ambientBlurContainer: document.getElementById('ambientBlurContainer'),
    screenEdgeLighting: document.getElementById('screenEdgeLighting'),
    neonThemeBtn: document.getElementById('neonThemeBtn'),
    neonThemeModal: document.getElementById('neonThemeModal'),
    closeNeonThemeBtn: document.getElementById('closeNeonThemeBtn'),
    edgeLightToggle: document.getElementById('edgeLightToggle'),
    themeToggleBtn: document.getElementById('themeToggleBtn'),
    scanBtn: document.getElementById('scanBtn'),
    scanIcon: document.getElementById('scanIcon'),
    settingsBtn: document.getElementById('settingsBtn'),
    settingsModal: document.getElementById('settingsModal'),
    closeSettingsBtn: document.getElementById('closeSettingsBtn'),
    ambientBlurToggle: document.getElementById('ambientBlurToggle'),
    keepScreenOnToggle: document.getElementById('keepScreenOnToggle'),
    spinningVinylToggle: document.getElementById('spinningVinylToggle'),
    btnReloadDemoTracks: document.getElementById('btnReloadDemoTracks'),
    btnRescanFromSettings: document.getElementById('btnRescanFromSettings'),
    btnClearHistoryFromSettings: document.getElementById('btnClearHistoryFromSettings'),
    openThemeFromSettings: document.getElementById('openThemeFromSettings'),
    demoSongsBanner: document.getElementById('demoSongsBanner'),
    playDemoNowBtn: document.getElementById('playDemoNowBtn'),
    searchInput: document.getElementById('searchInput'),
    clearSearchBtn: document.getElementById('clearSearchBtn'),
    sortSelect: document.getElementById('sortSelect'),
    permissionBanner: document.getElementById('permissionBanner'),
    grantPermissionBtn: document.getElementById('grantPermissionBtn'),
    sleepTimerBadge: document.getElementById('sleepTimerBadge'),
    sleepTimerBadgeText: document.getElementById('sleepTimerBadgeText'),
    cancelSleepBadgeBtn: document.getElementById('cancelSleepBadgeBtn'),
    songsCountText: document.getElementById('songsCountText'),
    songsDurationText: document.getElementById('songsDurationText'),
    songsList: document.getElementById('songsList'),
    foldersList: document.getElementById('foldersList'),
    pickSafFolderBtn: document.getElementById('pickSafFolderBtn'),
    favsList: document.getElementById('favsList'),
    historyList: document.getElementById('historyList'),
    clearHistoryBtn: document.getElementById('clearHistoryBtn'),
    newPlaylistInput: document.getElementById('newPlaylistInput'),
    createPlaylistBtn: document.getElementById('createPlaylistBtn'),
    playlistsList: document.getElementById('playlistsList'),
    artistsList: document.getElementById('artistsList'),
    albumsList: document.getElementById('albumsList'),
    // Mini Player
    miniPlayer: document.getElementById('miniPlayer'),
    miniProgressFill: document.getElementById('miniProgressFill'),
    miniPlayerTrigger: document.getElementById('miniPlayerTrigger'),
    miniArt: document.getElementById('miniArt'),
    miniArtPlaceholder: document.getElementById('miniArtPlaceholder'),
    miniTitle: document.getElementById('miniTitle'),
    miniArtist: document.getElementById('miniArtist'),
    miniPlayBtn: document.getElementById('miniPlayBtn'),
    miniPlayIcon: document.getElementById('miniPlayIcon'),
    miniNextBtn: document.getElementById('miniNextBtn'),
    // Now Playing Modal
    nowPlayingModal: document.getElementById('nowPlayingModal'),
    npCloseBtn: document.getElementById('npCloseBtn'),
    npHeaderAlbum: document.getElementById('npHeaderAlbum'),
    npFavoriteBtn: document.getElementById('npFavoriteBtn'),
    npArtwork: document.getElementById('npArtwork'),
    npArtPlaceholder: document.getElementById('npArtPlaceholder'),
    npArtCard: document.getElementById('npArtCard'),
    npSkipLeftCue: document.getElementById('npSkipLeftCue'),
    npSkipRightCue: document.getElementById('npSkipRightCue'),
    npSpectrumBar: document.getElementById('npSpectrumBar'),
    npTitle: document.getElementById('npTitle'),
    npArtist: document.getElementById('npArtist'),
    npSeekSlider: document.getElementById('npSeekSlider'),
    npCurrentTime: document.getElementById('npCurrentTime'),
    npTotalTime: document.getElementById('npTotalTime'),
    npShuffleBtn: document.getElementById('npShuffleBtn'),
    npPrevBtn: document.getElementById('npPrevBtn'),
    npRewind10Btn: document.getElementById('npRewind10Btn'),
    npPlayBtn: document.getElementById('npPlayBtn'),
    npPlayIcon: document.getElementById('npPlayIcon'),
    npForward10Btn: document.getElementById('npForward10Btn'),
    npNextBtn: document.getElementById('npNextBtn'),
    npRepeatBtn: document.getElementById('npRepeatBtn'),
    repeatOneBadge: document.getElementById('repeatOneBadge'),
    npSpeedBtn: document.getElementById('npSpeedBtn'),
    npSpeedText: document.getElementById('npSpeedText'),
    npLyricsBtn: document.getElementById('npLyricsBtn'),
    npLyricsPanel: document.getElementById('npLyricsPanel'),
    closeLyricsBtn: document.getElementById('closeLyricsBtn'),
    npSleepTimerBtn: document.getElementById('npSleepTimerBtn'),
    npEqShortcutBtn: document.getElementById('npEqShortcutBtn'),
    npQueueBtn: document.getElementById('npQueueBtn'),
    // Queue Modal
    queueModal: document.getElementById('queueModal'),
    queueList: document.getElementById('queueList'),
    clearQueueBtn: document.getElementById('clearQueueBtn'),
    closeQueueBtn: document.getElementById('closeQueueBtn'),
    // Equalizer
    eqPresetsList: document.getElementById('eqPresetsList'),
    eqBandsContainer: document.getElementById('eqBandsContainer'),
    bassBoostSlider: document.getElementById('bassBoostSlider'),
    bassBoostValueText: document.getElementById('bassBoostValueText'),
    // Sleep Timer Modal
    sleepTimerModal: document.getElementById('sleepTimerModal'),
    cancelSleepTimerBtn: document.getElementById('cancelSleepTimerBtn'),
    closeSleepTimerBtn: document.getElementById('closeSleepTimerBtn'),
    // Speed Modal
    speedModal: document.getElementById('speedModal'),
    closeSpeedBtn: document.getElementById('closeSpeedBtn'),
    // Song Context Modal
    songContextModal: document.getElementById('songContextModal'),
    contextArt: document.getElementById('contextArt'),
    contextTitle: document.getElementById('contextTitle'),
    contextArtist: document.getElementById('contextArtist'),
    ctxPlayNext: document.getElementById('ctxPlayNext'),
    ctxAddToQueue: document.getElementById('ctxAddToQueue'),
    ctxAddToPlaylist: document.getElementById('ctxAddToPlaylist'),
    ctxToggleFav: document.getElementById('ctxToggleFav'),
    ctxFavText: document.getElementById('ctxFavText'),
    ctxShare: document.getElementById('ctxShare'),
    ctxDetails: document.getElementById('ctxDetails'),
    ctxDelete: document.getElementById('ctxDelete'),
    // Details Modal
    songDetailsModal: document.getElementById('songDetailsModal'),
    detailsContent: document.getElementById('detailsContent'),
    closeDetailsBtn: document.getElementById('closeDetailsBtn'),
    // Add to Playlist Modal
    addToPlaylistModal: document.getElementById('addToPlaylistModal'),
    playlistSelectionList: document.getElementById('playlistSelectionList'),
    closeAddToPlaylistBtn: document.getElementById('closeAddToPlaylistBtn'),
    // Toast
    toastNotification: document.getElementById('toastNotification')
  };

  // --- Formatting Utilities ---
  function formatTime(ms) {
    if (!ms || isNaN(ms) || ms < 0) return '00:00';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    const pad = (n) => (n < 10 ? '0' + n : n);
    return `${pad(minutes)}:${pad(seconds)}`;
  }

  function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  function showToast(message) {
    const toast = elements.toastNotification;
    toast.textContent = message;
    toast.classList.add('show');
    clearTimeout(toast._timeout);
    toast._timeout = setTimeout(() => {
      toast.classList.remove('show');
    }, 2400);
  }

  // --- Initial Setup ---
  function init() {
    initTheme();
    bindEvents();
    checkPermissionsState();
    loadLibraryData();

    // Start periodic polling for playback state and timer
    setInterval(updatePlaybackTick, 800);
  }

  function initTheme() {
    const savedTheme = Bridge.getSetting('app_theme', 'dark');
    state.theme = savedTheme;
    document.documentElement.setAttribute('data-theme', savedTheme);

    const savedNeon = Bridge.getSetting('neon_theme', 'cyber');
    state.neonTheme = savedNeon;
    document.documentElement.setAttribute('data-neon', savedNeon);

    const edgeLightSaved = Bridge.getSetting('edge_light_enabled', 'true') === 'true';
    state.edgeLightEnabled = edgeLightSaved;
    if (elements.edgeLightToggle) elements.edgeLightToggle.checked = edgeLightSaved;
    if (elements.screenEdgeLighting) elements.screenEdgeLighting.classList.toggle('active', edgeLightSaved);

    // Ambient Blur Atmosphere Setting
    const ambientBlurSaved = Bridge.getSetting('ambient_blur_enabled', 'true') === 'true';
    state.ambientBlurEnabled = ambientBlurSaved;
    if (elements.ambientBlurToggle) elements.ambientBlurToggle.checked = ambientBlurSaved;
    if (elements.ambientBlurContainer) elements.ambientBlurContainer.classList.toggle('disabled', !ambientBlurSaved);

    // Keep Screen On Setting
    const keepScreenOnSaved = Bridge.getSetting('keep_screen_on', 'true') === 'true';
    state.keepScreenOn = keepScreenOnSaved;
    if (elements.keepScreenOnToggle) elements.keepScreenOnToggle.checked = keepScreenOnSaved;
    Bridge.setKeepScreenOn(keepScreenOnSaved);

    // Spinning Vinyl Setting
    const vinylSaved = Bridge.getSetting('spinning_vinyl_enabled', 'true') === 'true';
    state.spinningVinylEnabled = vinylSaved;
    if (elements.spinningVinylToggle) elements.spinningVinylToggle.checked = vinylSaved;
    if (elements.npArtCard) elements.npArtCard.classList.toggle('no-spin', !vinylSaved);

    // Seek Step Setting
    const seekStepSaved = parseInt(Bridge.getSetting('seek_step_seconds', '10')) || 10;
    state.seekStepSeconds = seekStepSaved;
    document.querySelectorAll('#seekStepGroup .settings-chip').forEach(chip => {
      const step = parseInt(chip.getAttribute('data-step'));
      chip.classList.toggle('active', step === seekStepSaved);
    });
    if (elements.npRewind10Btn) {
      const label = elements.npRewind10Btn.querySelector('.seek-label');
      if (label) label.textContent = `-${seekStepSaved}s`;
    }
    if (elements.npForward10Btn) {
      const label = elements.npForward10Btn.querySelector('.seek-label');
      if (label) label.textContent = `+${seekStepSaved}s`;
    }

    updateNeonThemeOptionUI(savedNeon);
  }

  function setNeonTheme(neonTheme) {
    state.neonTheme = neonTheme;
    document.documentElement.setAttribute('data-neon', neonTheme);
    Bridge.setSetting('neon_theme', neonTheme);
    updateNeonThemeOptionUI(neonTheme);
    showToast(`Neon Teması: ${getNeonThemeName(neonTheme)}`);
  }

  function updateNeonThemeOptionUI(activeTheme) {
    document.querySelectorAll('.neon-theme-option').forEach(opt => {
      opt.classList.toggle('active', opt.getAttribute('data-neon') === activeTheme);
    });
  }

  function getNeonThemeName(code) {
    const names = {
      cyber: 'Cyber Neon',
      violet: 'Elektrik Menekşe',
      emerald: 'Matrix Zümrüt',
      solar: 'Solar Ateş',
      gold: 'Neon Altın'
    };
    return names[code] || code;
  }

  function toggleTheme() {
    const newTheme = state.theme === 'dark' ? 'light' : 'dark';
    state.theme = newTheme;
    document.documentElement.setAttribute('data-theme', newTheme);
    Bridge.setSetting('app_theme', newTheme);
  }

  function checkPermissionsState() {
    const granted = Bridge.checkPermissions();
    if (!granted) {
      elements.permissionBanner.style.display = 'flex';
    } else {
      elements.permissionBanner.style.display = 'none';
    }
  }

  // --- Data Loading ---
  function loadLibraryData() {
    loadSongs();
    loadFolders();
    loadPlaylists();
    loadEqualizer();
  }

  function loadSongs() {
    let songs = [];
    if (state.searchQuery.trim().length > 0) {
      songs = Bridge.searchSongs(state.searchQuery.trim());
    } else {
      songs = Bridge.getSongs(state.sortOrder);
    }

    // If no songs found on device and not actively searching, load built-in synthwave demo tracks
    if ((!songs || songs.length === 0) && !state.searchQuery.trim()) {
      const demoTracks = Bridge.loadDemoTracks();
      if (demoTracks && demoTracks.length > 0) {
        songs = Bridge.getSongs(state.sortOrder);
      }
    }

    state.songs = songs;
    renderSongsList(songs);

    // Compute meta stats
    const totalMs = songs.reduce((acc, s) => acc + (s.duration || 0), 0);
    const totalMins = Math.round(totalMs / 60000);
    elements.songsCountText.textContent = `${songs.length} şarkı`;
    elements.songsDurationText.textContent = `${totalMins} dakika`;
  }

  function loadFolders() {
    state.folders = Bridge.getFolders();
    renderFoldersList(state.folders);
  }

  function loadPlaylists() {
    state.playlists = Bridge.getPlaylists();
    renderPlaylistsList(state.playlists);
    loadFavorites();
    loadHistory();
  }

  function loadFavorites() {
    state.favorites = Bridge.getFavorites();
    renderSongsIntoContainer(state.favorites, elements.favsList, 'Henüz favori şarkı eklenmedi.');
  }

  function loadHistory() {
    state.history = Bridge.getHistory();
    renderSongsIntoContainer(state.history, elements.historyList, 'Henüz dinleme geçmişi bulunmuyor.');
  }

  function loadArtistsAndAlbums() {
    state.artists = Bridge.getArtists();
    state.albums = Bridge.getAlbums();
    renderArtistsList(state.artists);
    renderAlbumsList(state.albums);
  }

  function loadEqualizer() {
    const eqData = Bridge.getEqualizerData();
    state.equalizerData = eqData;
    renderEqualizer(eqData);
  }

  // --- Rendering Functions ---
  function renderSongsList(songs) {
    renderSongsIntoContainer(songs, elements.songsList, 'Cihazınızda müzik dosyası bulunamadı. "Kitaplığı Yenile" butonunu deneyin.');
  }

  function renderSongsIntoContainer(songs, container, emptyMsg) {
    container.innerHTML = '';
    if (!songs || songs.length === 0) {
      container.innerHTML = `<div style="text-align:center; padding: 40px 16px; color: var(--text-tertiary); font-size: 14px;">${emptyMsg}</div>`;
      return;
    }

    const fragment = document.createDocumentFragment();
    songs.forEach((song, idx) => {
      const card = document.createElement('div');
      card.className = 'song-card';
      if (state.currentSong && state.currentSong.id === song.id) {
        card.classList.add('now-playing-item');
      }

      card.innerHTML = `
        <div class="song-card-art">
          ${song.albumArtUri ? `<img src="${song.albumArtUri}" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" alt="art">` : ''}
          <div class="art-icon" style="${song.albumArtUri ? 'display:none;' : ''}">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
            </svg>
          </div>
        </div>
        <div class="song-card-info">
          <div class="song-card-title">${escapeHtml(song.title)}</div>
          <div class="song-card-sub">${escapeHtml(song.artist)} • ${formatTime(song.duration)}</div>
        </div>
        <div class="song-card-actions">
          <button class="fav-icon-btn ${song.isFavorite ? 'active' : ''}" data-id="${song.id}" title="Favori">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
          </button>
          <button class="more-icon-btn" data-id="${song.id}" title="Seçenekler">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
            </svg>
          </button>
        </div>
      `;

      // Play on card click
      card.addEventListener('click', (e) => {
        if (e.target.closest('.fav-icon-btn') || e.target.closest('.more-icon-btn')) return;
        playQueueSong(songs, idx);
      });

      // Favorite toggle
      const favBtn = card.querySelector('.fav-icon-btn');
      favBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        const newFav = Bridge.toggleFavorite(song.id);
        song.isFavorite = newFav;
        favBtn.classList.toggle('active', newFav);
        if (state.currentSong && state.currentSong.id === song.id) {
          state.currentSong.isFavorite = newFav;
          updateNowPlayingFavoriteIcon(newFav);
        }
        showToast(newFav ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
      });

      // Context Menu
      const moreBtn = card.querySelector('.more-icon-btn');
      moreBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        openContextMenu(song);
      });

      fragment.appendChild(card);
    });

    container.appendChild(fragment);
  }

  function renderFoldersList(folders) {
    const container = elements.foldersList;
    container.innerHTML = '';
    if (!folders || folders.length === 0) {
      container.innerHTML = '<div style="text-align:center; padding: 40px; color: var(--text-tertiary);">Klasör bulunamadı.</div>';
      return;
    }

    folders.forEach(f => {
      const card = document.createElement('div');
      card.className = 'folder-card';
      card.innerHTML = `
        <div class="folder-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
            <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
          </svg>
        </div>
        <div class="folder-info">
          <div class="folder-name">${escapeHtml(f.displayName)}</div>
          <div class="folder-path">${escapeHtml(f.path)}</div>
        </div>
        <span class="folder-badge">${f.songCount} şarkı</span>
      `;

      card.addEventListener('click', () => {
        openFolderSongs(f);
      });

      container.appendChild(card);
    });
  }

  function openFolderSongs(folder) {
    const songs = Bridge.getFolderSongs(folder.path);
    // Switch to songs view filtered by this folder
    elements.searchInput.value = folder.displayName;
    state.searchQuery = folder.displayName;
    switchScreen('screenSongs');
    loadSongs();
  }

  function renderPlaylistsList(playlists) {
    const container = elements.playlistsList;
    container.innerHTML = '';
    if (!playlists || playlists.length === 0) {
      container.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 20px; color: var(--text-tertiary);">Henüz çalma listesi oluşturmadınız.</div>';
      return;
    }

    playlists.forEach(pl => {
      const card = document.createElement('div');
      card.className = 'grid-card';
      card.innerHTML = `
        <div class="grid-card-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
            <path d="M19 9H2v2h17V9zm0-4H2v2h17V5zM2 15h11v-2H2v2zm14 0v6l5-3-5-3z"/>
          </svg>
        </div>
        <div class="grid-card-title">${escapeHtml(pl.name)}</div>
        <div class="grid-card-sub">${pl.songCount || 0} şarkı</div>
      `;

      card.addEventListener('click', () => {
        openPlaylistDetails(pl);
      });

      container.appendChild(card);
    });
  }

  function openPlaylistDetails(pl) {
    const songs = Bridge.getPlaylistSongs(pl.id);
    if (songs.length === 0) {
      showToast('Bu listede henüz şarkı yok');
      return;
    }
    playQueueSong(songs, 0);
  }

  function renderArtistsList(artists) {
    const container = elements.artistsList;
    container.innerHTML = '';
    if (!artists || artists.length === 0) {
      container.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 20px; color: var(--text-tertiary);">Sanatçı bulunamadı.</div>';
      return;
    }

    artists.forEach(artist => {
      const card = document.createElement('div');
      card.className = 'grid-card';
      card.innerHTML = `
        <div class="grid-card-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
        </div>
        <div class="grid-card-title">${escapeHtml(artist)}</div>
      `;
      card.addEventListener('click', () => {
        const songs = Bridge.getArtistSongs(artist);
        playQueueSong(songs, 0);
      });
      container.appendChild(card);
    });
  }

  function renderAlbumsList(albums) {
    const container = elements.albumsList;
    container.innerHTML = '';
    if (!albums || albums.length === 0) {
      container.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 20px; color: var(--text-tertiary);">Albüm bulunamadı.</div>';
      return;
    }

    albums.forEach(album => {
      const card = document.createElement('div');
      card.className = 'grid-card';
      card.innerHTML = `
        <div class="grid-card-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z"/>
          </svg>
        </div>
        <div class="grid-card-title">${escapeHtml(album)}</div>
      `;
      card.addEventListener('click', () => {
        const songs = Bridge.getAlbumSongs(album);
        playQueueSong(songs, 0);
      });
      container.appendChild(card);
    });
  }

  function renderEqualizer(eqData) {
    if (!eqData || !eqData.supported) {
      elements.eqPresetsList.innerHTML = '<div style="color:var(--text-tertiary); font-size:13px;">Bu cihazda donanım ekolayzır desteklenmiyor veya aktif müzik oturumu bekleniyor.</div>';
      elements.eqBandsContainer.innerHTML = '';
      return;
    }

    // Presets
    const presets = eqData.presets || [];
    elements.eqPresetsList.innerHTML = '';
    presets.forEach((preset, idx) => {
      const chip = document.createElement('button');
      chip.className = `preset-chip ${eqData.currentPreset === idx ? 'active' : ''}`;
      chip.textContent = preset;
      chip.addEventListener('click', () => {
        Bridge.setEqualizerPreset(idx);
        document.querySelectorAll('.preset-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        // reload equalizer values
        setTimeout(loadEqualizer, 200);
      });
      elements.eqPresetsList.appendChild(chip);
    });

    // Bands
    const bands = eqData.bands || [];
    const minLevel = eqData.minLevel || -1500;
    const maxLevel = eqData.maxLevel || 1500;
    elements.eqBandsContainer.innerHTML = '';

    bands.forEach(b => {
      const col = document.createElement('div');
      col.className = 'eq-band-column';
      const levelDb = Math.round(b.level / 100);
      const freqLabel = b.centerFreq >= 1000 ? `${(b.centerFreq / 1000).toFixed(1)}k` : `${b.centerFreq}`;

      col.innerHTML = `
        <span class="eq-band-level">${levelDb > 0 ? '+' : ''}${levelDb} dB</span>
        <div class="eq-vertical-slider-wrapper">
          <input type="range" class="eq-vertical-slider" min="${minLevel}" max="${maxLevel}" value="${b.level}" data-band="${b.index}">
        </div>
        <span class="eq-band-freq">${freqLabel}Hz</span>
      `;

      const slider = col.querySelector('.eq-vertical-slider');
      const levelText = col.querySelector('.eq-band-level');

      slider.addEventListener('input', (e) => {
        const val = parseInt(e.target.value);
        const db = Math.round(val / 100);
        levelText.textContent = `${db > 0 ? '+' : ''}${db} dB`;
        Bridge.setEqualizerBandLevel(b.index, val);
      });

      elements.eqBandsContainer.appendChild(col);
    });

    // Bass Boost
    elements.bassBoostSlider.value = eqData.bassStrength || 0;
    elements.bassBoostValueText.textContent = `${Math.round((eqData.bassStrength || 0) / 10)}%`;
  }

  // --- Playback Handling ---
  function playQueueSong(queue, startIndex) {
    if (!queue || queue.length === 0) return;
    state.queue = queue;
    state.currentQueueIndex = startIndex;
    const song = queue[startIndex];
    state.currentSong = song;
    state.isPlaying = true;

    Bridge.playSong(song.id, queue, startIndex);
    updateUIWithCurrentSong(song);
  }

  function updateUIWithCurrentSong(song) {
    if (!song) return;

    // Mini Player
    elements.miniPlayer.style.display = 'flex';
    elements.miniTitle.textContent = song.title;
    elements.miniArtist.textContent = song.artist;
    if (song.albumArtUri) {
      elements.miniArt.src = song.albumArtUri;
      elements.miniArt.style.display = 'block';
      elements.miniArtPlaceholder.style.display = 'none';
    } else {
      elements.miniArt.style.display = 'none';
      elements.miniArtPlaceholder.style.display = 'flex';
    }

    // Now Playing Screen
    elements.npTitle.textContent = song.title;
    elements.npArtist.textContent = song.artist;
    elements.npHeaderAlbum.textContent = song.album || 'Neon Music Player';
    if (song.albumArtUri) {
      elements.npArtwork.src = song.albumArtUri;
      elements.npArtwork.style.display = 'block';
      elements.npArtPlaceholder.style.display = 'none';
    } else {
      elements.npArtwork.style.display = 'none';
      elements.npArtPlaceholder.style.display = 'flex';
    }
    updateNowPlayingFavoriteIcon(song.isFavorite);

    // Active item highlight in songs list
    document.querySelectorAll('.song-card').forEach(card => {
      card.classList.remove('now-playing-item');
    });
  }

  function updateNowPlayingFavoriteIcon(isFav) {
    elements.npFavoriteBtn.classList.toggle('active', !!isFav);
  }

  function showSeekCue(direction, text) {
    const cue = direction === 'left' ? elements.npSkipLeftCue : elements.npSkipRightCue;
    if (!cue) return;
    if (text) cue.textContent = text;
    cue.classList.add('show');
    setTimeout(() => {
      cue.classList.remove('show');
    }, 650);
  }

  function updatePlaybackTick() {
    const currentState = Bridge.getCurrentState();
    if (!currentState) return;

    state.isPlaying = currentState.isPlaying;
    state.position = currentState.position || 0;
    state.duration = currentState.duration || 0;
    state.isShuffle = currentState.isShuffle;
    state.repeatMode = currentState.repeatMode;
    state.playbackSpeed = currentState.playbackSpeed || 1.0;
    state.sleepTimerRemaining = currentState.sleepTimerRemaining || 0;

    if (currentState.hasSong && currentState.currentSong) {
      state.currentSong = currentState.currentSong;
      updateUIWithCurrentSong(state.currentSong);
    }

    // Update play/pause icons
    updatePlayPauseIcons(state.isPlaying);

    // Update seek slider and times if user is not actively scrubbing
    if (!state.isSeeking && state.duration > 0) {
      const progressPercent = (state.position / state.duration) * 100;
      elements.npSeekSlider.value = progressPercent;
      elements.miniProgressFill.style.width = `${progressPercent}%`;
      elements.npCurrentTime.textContent = formatTime(state.position);
      elements.npTotalTime.textContent = formatTime(state.duration);
    }

    // Update Shuffle & Repeat states
    elements.npShuffleBtn.classList.toggle('active', state.isShuffle);
    updateRepeatModeUI(state.repeatMode);

    // Speed text
    elements.npSpeedText.textContent = `${state.playbackSpeed.toFixed(1)}x`;

    // Sleep Timer badge
    if (state.sleepTimerRemaining > 0) {
      elements.sleepTimerBadge.style.display = 'inline-flex';
      const m = Math.floor(state.sleepTimerRemaining / 60);
      const s = state.sleepTimerRemaining % 60;
      elements.sleepTimerBadgeText.textContent = `Uyku: ${m < 10 ? '0' + m : m}:${s < 10 ? '0' + s : s}`;
    } else {
      elements.sleepTimerBadge.style.display = 'none';
    }
  }

  function updatePlayPauseIcons(isPlaying) {
    const playPath = "M8 5v14l11-7z";
    const pausePath = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";

    elements.miniPlayIcon.querySelector('path').setAttribute('d', isPlaying ? pausePath : playPath);
    elements.npPlayIcon.querySelector('path').setAttribute('d', isPlaying ? pausePath : playPath);

    // Update edge lighting
    if (elements.screenEdgeLighting) {
      elements.screenEdgeLighting.classList.toggle('is-playing', isPlaying && state.edgeLightEnabled);
    }

    // Update vinyl spinning and artwork halo
    if (elements.npArtCard) {
      if (isPlaying) {
        elements.npArtCard.classList.add('is-playing');
        elements.npArtCard.classList.remove('is-paused');
      } else {
        elements.npArtCard.classList.add('is-paused');
      }
    }

    const artworkContainer = document.querySelector('.np-artwork-container');
    if (artworkContainer) {
      artworkContainer.classList.toggle('is-playing', isPlaying);
    }

    if (elements.npSpectrumBar) {
      elements.npSpectrumBar.classList.toggle('is-playing', isPlaying);
    }
  }

  function updateRepeatModeUI(mode) {
    // 0: OFF, 1: ALL, 2: ONE
    if (mode === 2) {
      elements.npRepeatBtn.classList.add('active');
      elements.repeatOneBadge.style.display = 'flex';
    } else if (mode === 1) {
      elements.npRepeatBtn.classList.add('active');
      elements.repeatOneBadge.style.display = 'none';
    } else {
      elements.npRepeatBtn.classList.remove('active');
      elements.repeatOneBadge.style.display = 'none';
    }
  }

  // --- Context Menu Actions ---
  function openContextMenu(song) {
    state.selectedContextSong = song;
    elements.contextTitle.textContent = song.title;
    elements.contextArtist.textContent = `${song.artist} • ${song.album}`;
    if (song.albumArtUri) {
      elements.contextArt.src = song.albumArtUri;
      elements.contextArt.style.display = 'block';
    } else {
      elements.contextArt.style.display = 'none';
    }

    elements.ctxFavText.textContent = song.isFavorite ? 'Favorilerden Çıkar' : 'Favorilere Ekle';
    elements.songContextModal.style.display = 'flex';
  }

  function closeModals() {
    if (elements.songContextModal) elements.songContextModal.style.display = 'none';
    if (elements.queueModal) elements.queueModal.style.display = 'none';
    if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'none';
    if (elements.speedModal) elements.speedModal.style.display = 'none';
    if (elements.songDetailsModal) elements.songDetailsModal.style.display = 'none';
    if (elements.addToPlaylistModal) elements.addToPlaylistModal.style.display = 'none';
    if (elements.settingsModal) elements.settingsModal.style.display = 'none';
    if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'none';
  }

  function openQueueModal() {
    const currentState = Bridge.getCurrentState();
    const queue = (currentState && currentState.queue) ? currentState.queue : state.queue;
    elements.queueList.innerHTML = '';

    if (!queue || queue.length === 0) {
      elements.queueList.innerHTML = '<div style="text-align:center; padding:30px; color:var(--text-tertiary);">Kuyrukta şarkı bulunmuyor.</div>';
    } else {
      queue.forEach((item, idx) => {
        const row = document.createElement('div');
        row.className = 'song-card';
        if (state.currentSong && state.currentSong.id === item.id) {
          row.classList.add('now-playing-item');
        }
        row.innerHTML = `
          <div class="song-card-info">
            <div class="song-card-title">${escapeHtml(item.title)}</div>
            <div class="song-card-sub">${escapeHtml(item.artist)} • ${formatTime(item.duration)}</div>
          </div>
          <button class="icon-btn remove-queue-btn" data-index="${idx}" style="width:32px; height:32px;">✕</button>
        `;

        row.addEventListener('click', (e) => {
          if (e.target.closest('.remove-queue-btn')) return;
          Bridge.playSong(item.id, queue, idx);
          closeModals();
        });

        row.querySelector('.remove-queue-btn').addEventListener('click', (e) => {
          e.stopPropagation();
          Bridge.removeFromQueue(idx);
          row.remove();
        });

        elements.queueList.appendChild(row);
      });
    }

    elements.queueModal.style.display = 'flex';
  }

  function openSongDetails(song) {
    elements.detailsContent.innerHTML = `
      <div class="meta-row"><span class="meta-label">Şarkı Adı:</span><span class="meta-value">${escapeHtml(song.title)}</span></div>
      <div class="meta-row"><span class="meta-label">Sanatçı:</span><span class="meta-value">${escapeHtml(song.artist)}</span></div>
      <div class="meta-row"><span class="meta-label">Albüm:</span><span class="meta-value">${escapeHtml(song.album)}</span></div>
      <div class="meta-row"><span class="meta-label">Süre:</span><span class="meta-value">${formatTime(song.duration)}</span></div>
      <div class="meta-row"><span class="meta-label">Dosya Boyutu:</span><span class="meta-value">${formatFileSize(song.size)}</span></div>
      <div class="meta-row"><span class="meta-label">Format / MIME:</span><span class="meta-value">${escapeHtml(song.mimeType)}</span></div>
      <div class="meta-row"><span class="meta-label">Klasör:</span><span class="meta-value">${escapeHtml(song.folderName || 'Bilinmiyor')}</span></div>
      <div class="meta-row"><span class="meta-label">Dosya Yolu:</span><span class="meta-value">${escapeHtml(song.path || song.uri)}</span></div>
      <div class="meta-row"><span class="meta-label">Çalınma Sayısı:</span><span class="meta-value">${song.playCount || 0}</span></div>
    `;
    elements.songDetailsModal.style.display = 'flex';
  }

  function openAddToPlaylistPicker(song) {
    const playlists = Bridge.getPlaylists();
    elements.playlistSelectionList.innerHTML = '';

    if (!playlists || playlists.length === 0) {
      elements.playlistSelectionList.innerHTML = '<div style="padding:16px; color:var(--text-tertiary); text-align:center;">Önce Listeler ekranından bir çalma listesi oluşturun.</div>';
    } else {
      playlists.forEach(pl => {
        const item = document.createElement('div');
        item.className = 'choice-item';
        item.textContent = pl.name;
        item.addEventListener('click', () => {
          Bridge.addSongToPlaylist(pl.id, song.id);
          showToast(`"${pl.name}" listesine eklendi`);
          elements.addToPlaylistModal.style.display = 'none';
        });
        elements.playlistSelectionList.appendChild(item);
      });
    }

    elements.addToPlaylistModal.style.display = 'flex';
  }

  // --- Screen Navigation ---
  function switchScreen(screenId) {
    state.activeScreen = screenId;
    document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));

    const screenEl = document.getElementById(screenId);
    if (screenEl) screenEl.classList.add('active');

    const navBtn = document.querySelector(`.nav-item[data-screen="${screenId}"]`);
    if (navBtn) navBtn.classList.add('active');

    if (screenId === 'screenPlaylists') {
      loadPlaylists();
    } else if (screenId === 'screenLibrary') {
      loadArtistsAndAlbums();
    } else if (screenId === 'screenEqualizer') {
      loadEqualizer();
    }
  }

  // --- Event Binding ---
  function bindEvents() {
    // Theme toggle
    elements.themeToggleBtn.addEventListener('click', toggleTheme);

    // Neon Theme Modal toggle
    if (elements.neonThemeBtn) {
      elements.neonThemeBtn.addEventListener('click', () => {
        if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
      });
    }

    if (elements.closeNeonThemeBtn) {
      elements.closeNeonThemeBtn.addEventListener('click', () => {
        if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'none';
      });
    }

    // Neon theme option clicks
    document.querySelectorAll('.neon-theme-option').forEach(opt => {
      opt.addEventListener('click', () => {
        const themeId = opt.getAttribute('data-neon');
        if (themeId) setNeonTheme(themeId);
      });
    });

    // Edge Light switch toggle
    if (elements.edgeLightToggle) {
      elements.edgeLightToggle.addEventListener('change', (e) => {
        const isEnabled = e.target.checked;
        state.edgeLightEnabled = isEnabled;
        Bridge.setSetting('edge_light_enabled', isEnabled ? 'true' : 'false');
        if (elements.screenEdgeLighting) {
          elements.screenEdgeLighting.classList.toggle('active', isEnabled);
          elements.screenEdgeLighting.classList.toggle('is-playing', isEnabled && state.isPlaying);
        }
        showToast(isEnabled ? 'Kenar ışıklandırma açık' : 'Kenar ışıklandırma kapalı');
      });
    }

    // Refresh Library
    elements.scanBtn.addEventListener('click', () => {
      elements.scanIcon.style.animation = 'spin 0.8s linear infinite';
      Bridge.scanLibrary();
      showToast('Kitaplık taranıyor...');
    });

    // Grant Permission button
    elements.grantPermissionBtn.addEventListener('click', () => {
      Bridge.requestPermissions();
    });

    // Search Input
    let searchDebounce;
    elements.searchInput.addEventListener('input', (e) => {
      const q = e.target.value;
      state.searchQuery = q;
      elements.clearSearchBtn.style.display = q.length > 0 ? 'flex' : 'none';
      clearTimeout(searchDebounce);
      searchDebounce = setTimeout(() => {
        loadSongs();
      }, 200);
    });

    elements.clearSearchBtn.addEventListener('click', () => {
      elements.searchInput.value = '';
      state.searchQuery = '';
      elements.clearSearchBtn.style.display = 'none';
      loadSongs();
    });

    // Sort Select
    elements.sortSelect.addEventListener('change', (e) => {
      state.sortOrder = e.target.value;
      loadSongs();
    });

    // Bottom Navigation
    document.querySelectorAll('.nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const screenId = btn.getAttribute('data-screen');
        switchScreen(screenId);
      });
    });

    // Sub Tabs (Playlists / Library)
    document.querySelectorAll('.sub-tab-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const parent = btn.parentElement;
        parent.querySelectorAll('.sub-tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        const sub = btn.getAttribute('data-sub');
        if (sub === 'favs') {
          showSubContent('favsContainer');
          loadFavorites();
        } else if (sub === 'history') {
          showSubContent('historyContainer');
          loadHistory();
        } else if (sub === 'custom') {
          showSubContent('customPlaylistsContainer');
        } else if (sub === 'artists') {
          showSubContent('artistsContainer');
        } else if (sub === 'albums') {
          showSubContent('albumsContainer');
        }
      });
    });

    function showSubContent(containerId) {
      const target = document.getElementById(containerId);
      if (!target) return;
      const parent = target.parentElement;
      parent.querySelectorAll('.sub-content').forEach(c => c.classList.remove('active'));
      target.classList.add('active');
    }

    // SAF Picker
    elements.pickSafFolderBtn.addEventListener('click', () => {
      Bridge.pickFolder();
    });

    // Create Playlist
    elements.createPlaylistBtn.addEventListener('click', () => {
      const name = elements.newPlaylistInput.value.trim();
      if (!name) return;
      Bridge.createPlaylist(name);
      elements.newPlaylistInput.value = '';
      loadPlaylists();
      showToast(`"${name}" listesi oluşturuldu`);
    });

    // Clear History
    elements.clearHistoryBtn.addEventListener('click', () => {
      Bridge.clearHistory();
      loadHistory();
      showToast('Dinleme geçmişi temizlendi');
    });

    // Mini Player Trigger (open Now Playing)
    elements.miniPlayerTrigger.addEventListener('click', () => {
      elements.nowPlayingModal.classList.add('active');
    });

    elements.miniPlayBtn.addEventListener('click', () => {
      if (state.isPlaying) Bridge.pause();
      else Bridge.resume();
    });

    elements.miniNextBtn.addEventListener('click', () => {
      Bridge.next();
    });

    // Now Playing Close
    elements.npCloseBtn.addEventListener('click', () => {
      elements.nowPlayingModal.classList.remove('active');
    });

    // Now Playing Controls
    elements.npPlayBtn.addEventListener('click', () => {
      if (state.isPlaying) Bridge.pause();
      else Bridge.resume();
    });

    elements.npPrevBtn.addEventListener('click', () => {
      Bridge.previous();
    });

    elements.npNextBtn.addEventListener('click', () => {
      Bridge.next();
    });

    // 10s Rewind / Fast Forward
    if (elements.npRewind10Btn) {
      elements.npRewind10Btn.addEventListener('click', () => {
        const delta = -(state.seekStepSeconds || 10) * 1000;
        Bridge.seekBy(delta);
        showSeekCue('left', `-${state.seekStepSeconds || 10}s ⏪`);
      });
    }

    if (elements.npForward10Btn) {
      elements.npForward10Btn.addEventListener('click', () => {
        const delta = (state.seekStepSeconds || 10) * 1000;
        Bridge.seekBy(delta);
        showSeekCue('right', `⏩ +${state.seekStepSeconds || 10}s`);
      });
    }

    // Artwork quick seek on left/right tap
    if (elements.npArtCard) {
      elements.npArtCard.addEventListener('click', (e) => {
        const rect = elements.npArtCard.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        if (clickX < rect.width * 0.4) {
          const delta = -(state.seekStepSeconds || 10) * 1000;
          Bridge.seekBy(delta);
          showSeekCue('left', `-${state.seekStepSeconds || 10}s ⏪`);
        } else if (clickX > rect.width * 0.6) {
          const delta = (state.seekStepSeconds || 10) * 1000;
          Bridge.seekBy(delta);
          showSeekCue('right', `⏩ +${state.seekStepSeconds || 10}s`);
        }
      });
    }

    // Demo banner button
    if (elements.playDemoNowBtn) {
      elements.playDemoNowBtn.addEventListener('click', () => {
        const demoTracks = Bridge.loadDemoTracks();
        if (demoTracks && demoTracks.length > 0) {
          playQueueSong(demoTracks, 0);
          showToast('Demo parçası çalınıyor: ' + demoTracks[0].title);
        } else {
          const songs = Bridge.getSongs();
          if (songs && songs.length > 0) {
            playQueueSong(songs, 0);
            showToast('Çalınıyor: ' + songs[0].title);
          }
        }
      });
    }

    // Lyrics panel toggle
    if (elements.npLyricsBtn) {
      elements.npLyricsBtn.addEventListener('click', () => {
        if (elements.npLyricsPanel) {
          const isVisible = elements.npLyricsPanel.style.display === 'flex';
          elements.npLyricsPanel.style.display = isVisible ? 'none' : 'flex';
        }
      });
    }
    if (elements.closeLyricsBtn) {
      elements.closeLyricsBtn.addEventListener('click', () => {
        if (elements.npLyricsPanel) elements.npLyricsPanel.style.display = 'none';
      });
    }

    // Settings Modal
    if (elements.settingsBtn) {
      elements.settingsBtn.addEventListener('click', () => {
        if (elements.settingsModal) elements.settingsModal.style.display = 'flex';
      });
    }
    if (elements.closeSettingsBtn) {
      elements.closeSettingsBtn.addEventListener('click', () => {
        if (elements.settingsModal) elements.settingsModal.style.display = 'none';
      });
    }

    // Settings seek step selector
    document.querySelectorAll('#seekStepGroup .settings-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        document.querySelectorAll('#seekStepGroup .settings-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        const step = parseInt(chip.getAttribute('data-step')) || 10;
        state.seekStepSeconds = step;
        Bridge.setSetting('seek_step_seconds', String(step));
        if (elements.npRewind10Btn) {
          const label = elements.npRewind10Btn.querySelector('.seek-label');
          if (label) label.textContent = `-${step}s`;
        }
        if (elements.npForward10Btn) {
          const label = elements.npForward10Btn.querySelector('.seek-label');
          if (label) label.textContent = `+${step}s`;
        }
        showToast(`Atlama adımı: ${step} saniye`);
      });
    });

    // Settings Ambient blur toggle
    if (elements.ambientBlurToggle) {
      elements.ambientBlurToggle.addEventListener('change', (e) => {
        const isEnabled = e.target.checked;
        state.ambientBlurEnabled = isEnabled;
        Bridge.setSetting('ambient_blur_enabled', isEnabled ? 'true' : 'false');
        if (elements.ambientBlurContainer) {
          elements.ambientBlurContainer.classList.toggle('disabled', !isEnabled);
        }
        showToast(isEnabled ? 'Arka plan blur atmosferi açık' : 'Arka plan blur atmosferi kapalı');
      });
    }

    // Settings Keep screen on toggle
    if (elements.keepScreenOnToggle) {
      elements.keepScreenOnToggle.addEventListener('change', (e) => {
        const isEnabled = e.target.checked;
        state.keepScreenOn = isEnabled;
        Bridge.setKeepScreenOn(isEnabled);
        Bridge.setSetting('keep_screen_on', isEnabled ? 'true' : 'false');
        showToast(isEnabled ? 'Ekranı açık tutma aktif' : 'Ekranı açık tutma devre dışı');
      });
    }

    // Settings Vinyl spinning toggle
    if (elements.spinningVinylToggle) {
      elements.spinningVinylToggle.addEventListener('change', (e) => {
        const isEnabled = e.target.checked;
        state.spinningVinylEnabled = isEnabled;
        Bridge.setSetting('spinning_vinyl_enabled', isEnabled ? 'true' : 'false');
        if (elements.npArtCard) {
          elements.npArtCard.classList.toggle('no-spin', !isEnabled);
        }
        showToast(isEnabled ? 'Vinil plak animasyonu açık' : 'Vinil plak animasyonu kapalı');
      });
    }

    // Settings Reload demo tracks
    if (elements.btnReloadDemoTracks) {
      elements.btnReloadDemoTracks.addEventListener('click', () => {
        const tracks = Bridge.loadDemoTracks();
        loadSongs();
        showToast(`Örnek demo parçaları yüklendi (${tracks.length} adet)`);
        if (elements.settingsModal) elements.settingsModal.style.display = 'none';
        if (tracks && tracks.length > 0) {
          playQueueSong(tracks, 0);
        }
      });
    }

    // Settings Rescan
    if (elements.btnRescanFromSettings) {
      elements.btnRescanFromSettings.addEventListener('click', () => {
        Bridge.scanLibrary();
        showToast('Kütüphane taranıyor...');
        if (elements.settingsModal) elements.settingsModal.style.display = 'none';
      });
    }

    // Settings Clear history
    if (elements.btnClearHistoryFromSettings) {
      elements.btnClearHistoryFromSettings.addEventListener('click', () => {
        Bridge.clearHistory();
        loadHistory();
        showToast('Çalma geçmişi sıfırlandı');
      });
    }

    // Settings Open Theme Modal
    if (elements.openThemeFromSettings) {
      elements.openThemeFromSettings.addEventListener('click', () => {
        if (elements.settingsModal) elements.settingsModal.style.display = 'none';
        if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
      });
    }

    elements.npShuffleBtn.addEventListener('click', () => {
      const newShuffle = Bridge.toggleShuffle();
      state.isShuffle = newShuffle;
      elements.npShuffleBtn.classList.toggle('active', newShuffle);
      showToast(newShuffle ? 'Karışık çalma açık' : 'Karışık çalma kapalı');
    });

    elements.npRepeatBtn.addEventListener('click', () => {
      // Rotate 0 -> 1 -> 2 -> 0
      let nextMode = (state.repeatMode + 1) % 3;
      state.repeatMode = nextMode;
      Bridge.setRepeatMode(nextMode);
      updateRepeatModeUI(nextMode);
      const msgs = ['Tekrarlama kapalı', 'Tümünü tekrarla', 'Şarkıyı tekrarla'];
      showToast(msgs[nextMode]);
    });

    elements.npFavoriteBtn.addEventListener('click', () => {
      if (!state.currentSong) return;
      const newFav = Bridge.toggleFavorite(state.currentSong.id);
      state.currentSong.isFavorite = newFav;
      updateNowPlayingFavoriteIcon(newFav);
      showToast(newFav ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
      loadFavorites();
    });

    // Seek Slider
    elements.npSeekSlider.addEventListener('input', (e) => {
      state.isSeeking = true;
      const percent = parseFloat(e.target.value);
      const targetMs = (percent / 100) * state.duration;
      elements.npCurrentTime.textContent = formatTime(targetMs);
    });

    elements.npSeekSlider.addEventListener('change', (e) => {
      const percent = parseFloat(e.target.value);
      const targetMs = (percent / 100) * state.duration;
      Bridge.seek(Math.round(targetMs));
      state.isSeeking = false;
    });

    // Tool buttons in Now Playing
    elements.npQueueBtn.addEventListener('click', openQueueModal);
    elements.closeQueueBtn.addEventListener('click', () => {
      elements.queueModal.style.display = 'none';
    });
    elements.clearQueueBtn.addEventListener('click', () => {
      Bridge.clearQueue();
      elements.queueList.innerHTML = '<div style="text-align:center; padding:30px; color:var(--text-tertiary);">Kuyruk temizlendi.</div>';
    });

    // Sleep Timer
    elements.npSleepTimerBtn.addEventListener('click', () => {
      elements.sleepTimerModal.style.display = 'flex';
    });
    elements.closeSleepTimerBtn.addEventListener('click', () => {
      elements.sleepTimerModal.style.display = 'none';
    });
    elements.cancelSleepTimerBtn.addEventListener('click', () => {
      Bridge.cancelSleepTimer();
      elements.sleepTimerBadge.style.display = 'none';
      elements.sleepTimerModal.style.display = 'none';
      showToast('Zamanlayıcı iptal edildi');
    });
    elements.cancelSleepBadgeBtn.addEventListener('click', () => {
      Bridge.cancelSleepTimer();
      elements.sleepTimerBadge.style.display = 'none';
    });

    document.querySelectorAll('.timer-opt-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const mins = parseInt(btn.getAttribute('data-min'));
        Bridge.setSleepTimer(mins);
        elements.sleepTimerModal.style.display = 'none';
        showToast(`Müzik ${mins} dakika sonra kapanacak`);
      });
    });

    // Playback Speed
    elements.npSpeedBtn.addEventListener('click', () => {
      elements.speedModal.style.display = 'flex';
    });
    elements.closeSpeedBtn.addEventListener('click', () => {
      elements.speedModal.style.display = 'none';
    });

    document.querySelectorAll('.speed-opt-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const speed = parseFloat(btn.getAttribute('data-speed'));
        Bridge.setPlaybackSpeed(speed);
        state.playbackSpeed = speed;
        elements.npSpeedText.textContent = `${speed.toFixed(1)}x`;
        document.querySelectorAll('.speed-opt-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        elements.speedModal.style.display = 'none';
      });
    });

    // Equalizer Shortcut
    elements.npEqShortcutBtn.addEventListener('click', () => {
      elements.nowPlayingModal.classList.remove('active');
      switchScreen('screenEqualizer');
    });

    // Bass Boost Slider
    elements.bassBoostSlider.addEventListener('input', (e) => {
      const val = parseInt(e.target.value);
      elements.bassBoostValueText.textContent = `${Math.round(val / 10)}%`;
      Bridge.setBassBoost(val);
    });

    // Context Menu Buttons
    elements.ctxPlayNext.addEventListener('click', () => {
      if (state.selectedContextSong) Bridge.addToQueueNext(state.selectedContextSong);
      closeModals();
    });

    elements.ctxAddToQueue.addEventListener('click', () => {
      if (state.selectedContextSong) Bridge.addToQueueEnd(state.selectedContextSong);
      closeModals();
    });

    elements.ctxAddToPlaylist.addEventListener('click', () => {
      const song = state.selectedContextSong;
      closeModals();
      if (song) openAddToPlaylistPicker(song);
    });

    elements.ctxToggleFav.addEventListener('click', () => {
      if (state.selectedContextSong) {
        const newFav = Bridge.toggleFavorite(state.selectedContextSong.id);
        state.selectedContextSong.isFavorite = newFav;
        showToast(newFav ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
        loadFavorites();
        loadSongs();
      }
      closeModals();
    });

    elements.ctxShare.addEventListener('click', () => {
      if (state.selectedContextSong) Bridge.shareSong(state.selectedContextSong.id);
      closeModals();
    });

    elements.ctxDetails.addEventListener('click', () => {
      const song = state.selectedContextSong;
      closeModals();
      if (song) openSongDetails(song);
    });

    elements.ctxDelete.addEventListener('click', () => {
      if (state.selectedContextSong) {
        if (confirm(`"${state.selectedContextSong.title}" şarkısını silmek istediğinize emin misiniz?`)) {
          Bridge.deleteSong(state.selectedContextSong.id);
          showToast('Şarkı silindi');
          loadSongs();
        }
      }
      closeModals();
    });

    elements.closeDetailsBtn.addEventListener('click', () => {
      elements.songDetailsModal.style.display = 'none';
    });

    elements.closeAddToPlaylistBtn.addEventListener('click', () => {
      elements.addToPlaylistModal.style.display = 'none';
    });

    // Click outside backdrop to close modals
    document.querySelectorAll('.modal-backdrop').forEach(modal => {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          modal.style.display = 'none';
        }
      });
    });
  }

  // --- Android Back Handling ---
  window.handleAndroidBack = function () {
    // If Now Playing is open, close it (or close lyrics panel first if active)
    if (elements.npLyricsPanel && elements.npLyricsPanel.style.display !== 'none' && elements.npLyricsPanel.style.display !== '') {
      elements.npLyricsPanel.style.display = 'none';
      return true;
    }
    if (elements.nowPlayingModal.classList.contains('active')) {
      elements.nowPlayingModal.classList.remove('active');
      return true;
    }

    // If any modal is open, close it
    const openModals = [
      elements.settingsModal,
      elements.neonThemeModal,
      elements.songContextModal,
      elements.queueModal,
      elements.sleepTimerModal,
      elements.speedModal,
      elements.songDetailsModal,
      elements.addToPlaylistModal
    ];

    for (const m of openModals) {
      if (m.style.display !== 'none' && m.style.display !== '') {
        m.style.display = 'none';
        return true;
      }
    }

    // If not in Songs screen, navigate back to Songs
    if (state.activeScreen !== 'screenSongs') {
      switchScreen('screenSongs');
      return true;
    }

    return false; // Let Android finish or minimize
  };

  // --- Bridge Event Handlers from Native ---
  window.onPlaybackStateUpdated = function (escapedJson) {
    try {
      const stateObj = JSON.parse(escapedJson);
      state.isPlaying = stateObj.isPlaying;
      state.position = stateObj.position;
      state.duration = stateObj.duration;
      state.isShuffle = stateObj.isShuffle;
      state.repeatMode = stateObj.repeatMode;
      state.playbackSpeed = stateObj.playbackSpeed || 1.0;
      state.sleepTimerRemaining = stateObj.sleepTimerRemaining || 0;

      if (stateObj.hasSong && stateObj.currentSong) {
        state.currentSong = stateObj.currentSong;
        updateUIWithCurrentSong(state.currentSong);
      }

      updatePlayPauseIcons(state.isPlaying);
    } catch (e) {
      console.error('onPlaybackStateUpdated parse error:', e);
    }
  };

  window.onScanCompleted = function (count) {
    elements.scanIcon.style.animation = 'none';
    showToast(`Tarama tamamlandı: ${count} şarkı bulundu`);
    loadLibraryData();
  };

  window.onScanFailed = function (err) {
    elements.scanIcon.style.animation = 'none';
    showToast('Tarama hatası: ' + err);
  };

  window.onPermissionResult = function (granted) {
    checkPermissionsState();
    if (granted) {
      showToast('Erişim izni verildi');
      loadLibraryData();
    } else {
      showToast('İzin reddedildi. Şarkılar listelenemeyebilir.');
    }
  };

  window.onSafFolderScanned = function (count, uri) {
    showToast(`Özel klasörden ${count} şarkı eklendi`);
    loadLibraryData();
  };

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // Run on DOM loaded
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
