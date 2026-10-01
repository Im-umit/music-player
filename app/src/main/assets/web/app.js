/**
 * Müzik Çalar - Professional Android Music Player Web Frontend
 * Native Kotlin Bridge + Media3 + Room Database Integration
 */

(function () {
  'use strict';

  const tr = (key) => (window.i18n && typeof window.i18n.t === 'function') ? window.i18n.t(key) : key;
  const songCountLabel = (count) => `${Number(count) || 0} ${tr('songsUnit')}`;

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

    cancelScan: function () {
      if (this.isAvailable() && typeof window.AndroidMusicBridge.cancelScan === 'function') {
        window.AndroidMusicBridge.cancelScan();
      }
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
        const queueIds = Array.isArray(queue) ? queue.map(song => Number(song?.id)).filter(id => Number.isFinite(id) && id > 0) : [];
        window.AndroidMusicBridge.playSong(songId, JSON.stringify(queueIds), index);
      }
    },

    playAllShuffled: function (queue) {
      if (this.isAvailable()) {
        const queueIds = Array.isArray(queue) ? queue.map(song => Number(song?.id)).filter(id => Number.isFinite(id) && id > 0) : [];
        window.AndroidMusicBridge.playAllShuffled(JSON.stringify(queueIds));
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
      if (this.isAvailable()) return window.AndroidMusicBridge.removeSongFromPlaylist(playlistId, songId);
      return false;
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
    moveSongInPlaylist: function (playlistId, songId, newIndex) {
      if (this.isAvailable()) return !!window.AndroidMusicBridge.moveSongInPlaylist(playlistId, songId, newIndex);
      return false;
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

    getAlbumSongs: function (album, artist = '') {
      if (this.isAvailable()) {
        try {
          return JSON.parse(window.AndroidMusicBridge.getAlbumSongs(album, artist));
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

    pickSafFolder: function () {
      this.pickFolder();
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
    },

    fetchLyricsAsync: function (title, artist, album, durationSec, requestId) {
      if (this.isAvailable() && typeof window.AndroidMusicBridge.fetchLyricsAsync === 'function') {
        window.AndroidMusicBridge.fetchLyricsAsync(title || '', artist || '', album || '', durationSec || 0, requestId);
        return true;
      }
      return false;
    }
  };

  // --- App State ---
  const state = {
    songs: [],
    librarySongs: [],
    librarySongsLoaded: false,
    searchIndex: new Map(),
    folderFilter: null,
    folders: [],
    playlists: [],
    favorites: [],
    history: [],
    artists: [],
    albums: [],
    pinnedArtists: [],
    albumCounts: new Map(),
    activeCollection: null,
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
    searchFilter: 'all',
    sortOrder: 'title_asc',
    isSeeking: false,
    selectedContextSong: null,
    sleepTimerRemaining: 0,
    sleepTimerInterval: null,
    equalizerData: null,
    theme: 'dark',
    neonTheme: 'cyber',
    edgeLightEnabled: false,
    seekStepSeconds: 10,
    ambientBlurEnabled: true,
    keepScreenOn: true,
    spinningVinylEnabled: false,
    // Synchronized LRC Lyrics state
    lyrics: [],
    isLyricsTabActive: false,
    currentLyricIndex: -1,
    currentLyricsSongId: null,
    userScrolledLyricsUntil: 0,
    scanWatchdog: null
  };

  // --- DOM Elements ---
  const $ = (id) => document.getElementById(id);

  const elements = {
    app: $('app'),
    ambientBlurContainer: $('ambientBlurContainer'),
    screenEdgeLighting: $('screenEdgeLighting'),
    neonThemeBtn: $('neonThemeBtn'),
    neonThemeModal: $('neonThemeModal'),
    closeNeonThemeBtn: $('closeNeonThemeBtn'),
    edgeLightToggle: $('edgeLightToggle'),
    themeToggleBtn: $('themeToggleBtn'),
    scanBtn: $('drawerRescanBtn') || $('btnRescanFromSettings') || $('scanBtn'),
    scanIcon: $('scanIcon'),
    settingsBtn: $('settingsBtn'),
    settingsModal: $('settingsModal'),
    closeSettingsBtn: $('closeSettingsBtn'),
    ambientBlurToggle: $('ambientBlurToggle'),
    keepScreenOnToggle: $('keepScreenOnToggle'),
    spinningVinylToggle: $('spinningVinylToggle'),
    btnPickSafFromSettings: $('btnPickSafFromSettings'),
    btnRescanFromSettings: $('btnRescanFromSettings'),
    btnClearHistoryFromSettings: $('btnClearHistoryFromSettings'),
    openThemeFromSettings: $('openThemeFromSettings'),
    searchInput: $('dedicatedSearchInput') || $('searchInput'),
    clearSearchBtn: $('clearDedicatedSearchBtn') || $('clearSearchBtn'),
    sortSelect: $('sortSelect'),
    permissionBanner: $('permissionBanner'),
    grantPermissionBtn: $('grantPermissionBtn'),
    sleepTimerBadge: $('sleepTimerBadge'),
    sleepTimerBadgeText: $('sleepTimerBadgeText'),
    cancelSleepBadgeBtn: $('cancelSleepBadgeBtn'),
    songsCountText: $('songsCountText'),
    songsDurationText: $('songsDurationText'),
    songsList: $('songsList'),
    foldersList: $('foldersList'),
    pickSafFolderBtn: $('pickSafFolderBtn'),
    favsList: $('favsList'),
    historyList: $('historyList'),
    clearHistoryBtn: $('clearHistoryBtn'),
    newPlaylistInput: $('newPlaylistInput'),
    createPlaylistBtn: $('createPlaylistBtn'),
    playlistsList: $('playlistsList'),
    artistsList: $('artistsList'),
    albumsList: $('albumsList'),
    // Top Navigation & Actions (Screenshot 3)
    headerMenuBtn: $('drawerToggleBtn') || $('headerMenuBtn'),
    summaryPillBtn: $('viewSummaryBtn') || $('summaryPillBtn'),
    headerSortBtn: $('openSortModalBtn') || $('headerSortBtn'),
    headerSearchBtn: $('openSearchBtn') || $('headerSearchBtn'),
    btnShuffleAll: $('btnShuffleAll'),
    btnPlayAll: $('btnPlayAll'),
    // Side Drawer
    sideDrawerBackdrop: $('drawerBackdrop') || $('sideDrawerBackdrop'),
    closeDrawerBtn: $('closeDrawerBtn'),
    drawerSongsBtn: $('drawerSongsBtn'),
    drawerPlaylistsBtn: $('drawerPlaylistsBtn'),
    drawerFoldersBtn: $('drawerFoldersBtn'),
    drawerAlbumsBtn: $('drawerAlbumsBtn'),
    drawerArtistsBtn: $('drawerArtistsBtn'),
    drawerEqBtn: $('drawerEqBtn'),
    drawerSleepBtn: $('drawerSleepBtn'),
    drawerRescanBtn: $('drawerRescanBtn'),
    drawerThemeBtn: $('drawerThemeBtn'),
    drawerSettingsBtn: $('drawerSettingsBtn'),
    drawerSummaryBtn: $('drawerSummaryBtn'),
    // Summary Modal ("Özetini Görüntüle")
    summaryModal: $('summaryModal'),
    closeSummaryBtn: $('closeSummaryBtn'),
    summaryCloseActionBtn: $('summaryCloseActionBtn') || $('closeSummaryBtn'),
    summarySongsCount: $('sumTotalSongs') || $('summarySongsCount'),
    summaryDurationText: $('sumTotalDuration') || $('summaryDurationText'),
    summaryArtistsCount: $('sumTotalArtists') || $('summaryArtistsCount'),
    summaryAlbumsCount: $('sumTotalAlbums') || $('summaryAlbumsCount'),
    summaryFoldersCount: $('sumTotalFolders') || $('summaryFoldersCount'),
    summaryFavoritesCount: $('sumTotalFavs') || $('summaryFavoritesCount'),
    // Sort Bottom Sheet (Screenshot 4)
    sortModal: $('sortModal'),
    cancelSortBtn: $('cancelSortBtn'),
    applySortBtn: $('applySortBtn'),
    // Dedicated Search Screen (Screenshot 6)
    searchScreen: $('fullSearchScreen') || $('searchScreen'),
    searchScreenBackBtn: $('closeSearchScreenBtn') || $('searchScreenBackBtn'),
    fullSearchInput: $('dedicatedSearchInput') || $('fullSearchInput'),
    clearFullSearchBtn: $('clearDedicatedSearchBtn') || $('clearFullSearchBtn'),
    fullSearchResults: $('searchResultsList') || $('fullSearchResults'),
    // Mini Player
    miniPlayer: $('miniPlayer'),
    miniProgressFill: $('miniProgressFill'),
    miniPlayerTrigger: $('miniPlayerTrigger'),
    miniArt: $('miniArt'),
    miniArtPlaceholder: $('miniArtPlaceholder'),
    miniTitle: $('miniTitle'),
    miniArtist: $('miniArtist'),
    miniPlayBtn: $('miniPlayBtn'),
    miniPlayIcon: $('miniPlayIcon'),
    miniNextBtn: $('miniNextBtn'),
    miniQueueBtn: $('miniQueueBtn'),
    // Now Playing Modal (Screenshot 5)
    nowPlayingModal: $('nowPlayingModal'),
    npFullBlurredBg: $('npFullBlurredBg'),
    npCloseBtn: $('npCloseBtn'),
    npSegSongBtn: $('tabNpCover') || $('npSegSongBtn'),
    npSegLyricsBtn: $('tabNpLyrics') || $('npSegLyricsBtn'),
    npHeaderThemeBtn: $('npHeaderThemeBtn'),
    npHeaderOptionsBtn: $('npMoreBtn') || $('npHeaderOptionsBtn'),
    npHeaderAlbum: $('npHeaderAlbum'),
    npArtworkContainer: $('npArtworkContainer'),
    npArtwork: $('npArtwork'),
    npArtPlaceholder: $('npArtPlaceholder'),
    npArtCard: $('npArtCard'),
    npSkipLeftCue: $('npSkipLeftCue'),
    npSkipRightCue: $('npSkipRightCue'),
    npSpectrumBar: $('npSpectrumBar'),
    npTitle: $('npTitle'),
    npArtist: $('npArtist'),
    npFavQuickBtn: $('npFavoriteBtn') || $('npFavQuickBtn'),
    npAddPlaylistBtn: $('npAddToPlaylistBtn') || $('npAddPlaylistBtn'),
    npEqQuickBtn: $('npEqShortcutBtn') || $('npEqQuickBtn'),
    npTimerQuickBtn: $('npSleepTimerBtn') || $('npTimerQuickBtn'),
    npQueueQuickBtn: $('npQueueBtn') || $('npQueueQuickBtn'),
    npStepBackBtn: $('npRewind10Btn') || $('npStepBackBtn'),
    npStepForwardBtn: $('npForward10Btn') || $('npStepForwardBtn'),
    npSeekSlider: $('npSeekSlider'),
    npCurrentTime: $('npCurrentTime'),
    npTotalTime: $('npTotalTime'),
    npShuffleBtn: $('npShuffleBtn'),
    npPrevBtn: $('npPrevBtn'),
    npRewind10Btn: $('npRewind10Btn'),
    npPlayBtn: $('npPlayBtn'),
    npPlayIcon: $('npPlayIcon'),
    npForward10Btn: $('npForward10Btn'),
    npNextBtn: $('npNextBtn'),
    npRepeatBtn: $('npRepeatBtn'),
    repeatOneBadge: $('repeatOneBadge'),
    npSpeedBtn: $('npSpeedBtn'),
    npSpeedText: $('npSpeedText'),
    npLyricsBtn: $('tabNpLyrics') || $('npLyricsBtn'),
    npLyricsPanel: $('npLyricsPanel'),
    lyricsContent: $('lyricsContent'),
    closeLyricsBtn: $('closeLyricsBtn'),
    npSleepTimerBtn: $('npSleepTimerBtn'),
    npEqShortcutBtn: $('npEqShortcutBtn'),
    npQueueBtn: $('npQueueBtn'),
    // Queue Modal
    queueModal: $('queueModal'),
    queueList: $('queueList'),
    clearQueueBtn: $('clearQueueBtn'),
    closeQueueBtn: $('closeQueueBtn'),
    // Equalizer
    eqPresetsList: $('eqPresetsList'),
    eqBandsContainer: $('eqBandsContainer'),
    bassBoostSlider: $('bassBoostSlider'),
    bassBoostValueText: $('bassBoostValueText'),
    // Sleep Timer Modal
    sleepTimerModal: $('sleepTimerModal'),
    cancelSleepTimerBtn: $('cancelSleepTimerBtn'),
    closeSleepTimerBtn: $('closeSleepTimerBtn'),
    // Speed Modal
    speedModal: $('speedModal'),
    closeSpeedBtn: $('closeSpeedBtn'),
    // Song Context Modal
    songContextModal: $('songContextModal'),
    contextArt: $('contextArt'),
    contextTitle: $('contextTitle'),
    contextArtist: $('contextArtist'),
    ctxPlayNext: $('ctxPlayNext'),
    ctxAddToQueue: $('ctxAddToQueue'),
    ctxAddToPlaylist: $('ctxAddToPlaylist'),
    ctxToggleFav: $('ctxToggleFav'),
    ctxFavText: $('ctxFavText'),
    ctxShare: $('ctxShare'),
    ctxDetails: $('ctxDetails'),
    ctxDelete: $('ctxDelete'),
    // Details Modal
    songDetailsModal: $('songDetailsModal'),
    detailsContent: $('detailsContent'),
    closeDetailsBtn: $('closeDetailsBtn'),
    // Add to Playlist Modal
    addToPlaylistModal: $('addToPlaylistModal'),
    playlistSelectionList: $('playlistSelectionList'),
    closeAddToPlaylistBtn: $('closeAddToPlaylistBtn'),
    // Toast
    toastNotification: $('toastNotification'),
    // Splash & Auto-Scan Elements
    appSplashScreen: $('appSplashScreen'),
    splashProgressFill: $('splashProgressFill'),
    splashStatusText: $('splashStatusText'),
    scanProgressBar: $('scanProgressBar')
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

  function getSafeArtUri(song) {
    if (!song) return '';
    const uri = String(song.albumArtUri || '').trim();
    if (!uri || uri.startsWith('content://')) {
      return song.id ? `https://music.local/albumart/${song.id}` : '';
    }
    try {
      const parsed = new URL(uri, window.location.href);
      if (parsed.protocol === 'https:' && parsed.hostname === 'music.local' && parsed.pathname.startsWith('/albumart/')) {
        return parsed.toString();
      }
    } catch (_) {}
    // Song artwork is native/local data. Do not allow arbitrary remote URLs from
    // metadata to turn into background network requests from the WebView.
    return song.id ? `https://music.local/albumart/${song.id}` : '';
  }

  let currentArtSessionId = 0;

  function updateArtworkElement(imgEl, placeholderEl, song) {
    if (!imgEl) return;
    const artUri = getSafeArtUri(song);
    const targetSongId = song ? song.id : null;
    const sessionId = ++currentArtSessionId;

    imgEl.onload = null;
    imgEl.onerror = null;

    if (!artUri) {
      imgEl.style.display = 'none';
      imgEl.removeAttribute('src');
      if (placeholderEl) placeholderEl.style.display = 'flex';
      return;
    }

    const versionParam = `v=${song.dateModified || song.id || ''}`;
    const fullUri = artUri.includes('?') ? `${artUri}&${versionParam}` : `${artUri}?${versionParam}`;

    imgEl.style.display = 'none';
    if (placeholderEl) placeholderEl.style.display = 'flex';

    imgEl.onload = function () {
      // Prevent async artwork race conditions
      if (sessionId !== currentArtSessionId) return;
      if (state.currentSong && targetSongId && state.currentSong.id !== targetSongId && (imgEl === elements.npArtwork || imgEl === elements.miniArt)) {
        return;
      }
      imgEl.style.display = 'block';
      if (placeholderEl) placeholderEl.style.display = 'none';
    };

    imgEl.onerror = function () {
      if (sessionId !== currentArtSessionId) return;
      imgEl.style.display = 'none';
      imgEl.removeAttribute('src');
      if (placeholderEl) placeholderEl.style.display = 'flex';
    };

    imgEl.src = fullUri;
  }

  function updateNowPlayingBlurredBackground(song) {
    const bgEl = elements.npFullBlurredBg || $('npFullBlurredBg');
    if (!bgEl) return;

    const targetSongId = song ? song.id : null;
    const sessionId = ++currentArtSessionId;

    if (!song) {
      bgEl.style.backgroundImage = 'none';
      bgEl.style.backgroundColor = 'var(--bg-primary)';
      return;
    }

    const artUri = getSafeArtUri(song);
    if (!artUri) {
      const hue = Math.abs((song.id || 1) * 67) % 360;
      bgEl.style.backgroundImage = `linear-gradient(135deg, hsl(${hue}, 55%, 15%), hsl(${(hue + 45) % 360}, 60%, 8%))`;
      return;
    }

    const versionParam = `v=${song.dateModified || song.id || ''}`;
    const fullUri = artUri.includes('?') ? `${artUri}&${versionParam}` : `${artUri}?${versionParam}`;

    const tempImg = new Image();
    tempImg.onload = function () {
      if (sessionId !== currentArtSessionId) return;
      if (state.currentSong && targetSongId && state.currentSong.id !== targetSongId) {
        return;
      }
      bgEl.style.backgroundImage = `url("${fullUri}")`;
    };
    tempImg.onerror = function () {
      if (sessionId !== currentArtSessionId) return;
      const hue = Math.abs((song.id || 1) * 67) % 360;
      bgEl.style.backgroundImage = `linear-gradient(135deg, hsl(${hue}, 55%, 15%), hsl(${(hue + 45) % 360}, 60%, 8%))`;
    };
    tempImg.src = fullUri;
  }

  // --- Splash Screen & Launch Animation ---
  function initSplashScreen() {
    const splash = elements.appSplashScreen;
    if (!splash) return;

    let progress = 20;
    const progressFill = elements.splashProgressFill;
    const statusText = elements.splashStatusText;

    if (progressFill) progressFill.style.width = '20%';

    const stepInterval = setInterval(() => {
      progress += Math.floor(Math.random() * 18) + 14;
      if (progress > 92) progress = 92;
      if (progressFill) progressFill.style.width = `${progress}%`;

      if (progress > 35 && progress < 75) {
        if (statusText) statusText.textContent = tr('scanningMusic');
      } else if (progress >= 75) {
        if (statusText) statusText.textContent = tr('preparingLibrary');
      }
    }, 180);

    setTimeout(() => {
      clearInterval(stepInterval);
      if (progressFill) progressFill.style.width = '100%';
      if (statusText) statusText.textContent = tr('ready');

      setTimeout(() => {
        splash.classList.add('splash-exit');
        setTimeout(() => {
          splash.style.display = 'none';
        }, 650);
      }, 300);
    }, 1300);
  }

  // --- Auto-Scan State Management ---
  function showScanningState(isScanning) {
    state.isScanning = isScanning;
    if (state.scanWatchdog) {
      clearTimeout(state.scanWatchdog);
      state.scanWatchdog = null;
    }
    if (isScanning) {
      // Manual scans should never leave the UI in an endless loading state.
      state.scanWatchdog = setTimeout(() => {
        state.scanWatchdog = null;
        if (!state.isScanning) return;
        Bridge.cancelScan();
        state.isScanning = false;
        if (elements.scanProgressBar) elements.scanProgressBar.classList.remove('active');
        if (elements.scanIcon) elements.scanIcon.style.animation = 'none';
        showToast(tr('scanTimeout'));
        loadSongs(true);
      }, 120000);
    }
    if (elements.scanProgressBar) {
      elements.scanProgressBar.classList.toggle('active', isScanning);
    }
    if (elements.scanIcon) {
      elements.scanIcon.style.animation = isScanning ? 'spin 1s linear infinite' : 'none';
    }
    if (elements.songsCountText) {
      if (isScanning && (!state.songs || state.songs.length === 0)) {
        elements.songsCountText.textContent = tr('scanningMusic');
      } else if (state.songs) {
        elements.songsCountText.textContent = `${state.songs.length} Şarkı`;
      }
    }
    if ((!state.songs || state.songs.length === 0) && elements.songsList) {
      renderSongsList([]);
    }
  }

  // --- Initial Setup ---
  function init() {
    initTheme();
    bindEvents();
    initSplashScreen();
    checkPermissionsState();

    const hasPermission = Bridge.checkPermissions();
    loadLibraryData();

    if (hasPermission) {
      if (!state.songs || state.songs.length === 0) {
        showScanningState(true);
        Bridge.scanLibrary();
      } else {
        // Silently scan for newly added tracks in the background
        Bridge.scanLibrary();
      }
    }

    // Fetch initial playback state immediately
    updatePlaybackTick();

    // Periodic safety fallback polling (1 second)
    setInterval(() => {
      if (Date.now() - lastNativePlaybackUpdateAt > 5000) updatePlaybackTick();
    }, 5000);
  }

  function initTheme() {
    const savedTheme = Bridge.getSetting('app_theme', 'dark');
    state.theme = savedTheme;
    document.documentElement.setAttribute('data-theme', savedTheme);

    const savedNeon = Bridge.getSetting('neon_theme', 'violet');
    state.neonTheme = savedNeon;
    document.documentElement.setAttribute('data-neon', savedNeon);

    const edgeLightSaved = Bridge.getSetting('edge_light_enabled', 'false') === 'true';
    state.edgeLightEnabled = edgeLightSaved;
    if (elements.edgeLightToggle) elements.edgeLightToggle.checked = edgeLightSaved;
    if (elements.screenEdgeLighting) {
      elements.screenEdgeLighting.classList.toggle('active', edgeLightSaved);
      elements.screenEdgeLighting.classList.toggle('is-playing', edgeLightSaved && state.isPlaying);
    }

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

    // App Language Setting
    if (window.i18n) {
      const savedLanguage = Bridge.getSetting('app_language', window.i18n.getLanguage());
      if (window.i18n.supportedLanguages.includes(savedLanguage)) {
        window.i18n.setLanguage(savedLanguage, { persistNative: false });
      }
      const currentLang = window.i18n.getLanguage();
      const langSelect = document.getElementById('appLanguageSelect');
      if (langSelect) langSelect.value = currentLang;
      window.i18n.applyTranslations();
    }

    // Saved Sort Order
    const savedSort = Bridge.getSetting('sort_order', 'date_added_desc');
    state.sortOrder = savedSort;

    // Saved pinned artists
    try {
      const rawPinnedArtists = Bridge.getSetting('pinned_artists', '[]');
      const parsedPinnedArtists = JSON.parse(rawPinnedArtists);
      state.pinnedArtists = Array.isArray(parsedPinnedArtists) ? parsedPinnedArtists.filter(Boolean) : [];
    } catch (e) {
      state.pinnedArtists = [];
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
    if (elements.permissionBanner) {
      elements.permissionBanner.style.display = !granted ? 'flex' : 'none';
    }
  }

  // --- Data Loading ---
  function loadLibraryData() {
    // Keep startup/resume cheap: the song dataset is the shared source for search and
    // playback, while heavier collection screens are loaded only when they are visible.
    loadSongs();
    switch (state.activeScreen) {
      case 'screenPlaylists':
        loadPlaylists();
        break;
      case 'screenFolders':
        loadFolders();
        break;
      case 'screenAlbums':
      case 'screenArtists':
        loadArtistsAndAlbums();
        break;
      case 'screenEqualizer':
        loadEqualizer();
        break;
    }
  }

  function sortSongsArray(arr, sortOrder) {
    if (!arr || !Array.isArray(arr)) return [];
    const copy = [...arr];
    const parts = (sortOrder || 'date_added_desc').split('_');
    const isAsc = parts[parts.length - 1] === 'asc';
    const field = parts.slice(0, parts.length - 1).join('_');

    copy.sort((a, b) => {
      let cmp = 0;
      switch (field) {
        case 'title':
          cmp = (a.title || '').localeCompare(b.title || '', 'tr', { sensitivity: 'base' });
          break;
        case 'artist':
          cmp = (a.artist || '').localeCompare(b.artist || '', 'tr', { sensitivity: 'base' });
          break;
        case 'album':
          cmp = (a.album || '').localeCompare(b.album || '', 'tr', { sensitivity: 'base' });
          break;
        case 'folder':
          cmp = (a.folderName || '').localeCompare(b.folderName || '', 'tr', { sensitivity: 'base' });
          break;
        case 'date':
        case 'date_added':
          cmp = (a.dateAdded || 0) - (b.dateAdded || 0);
          break;
        case 'play':
        case 'play_count':
          cmp = (a.playCount || 0) - (b.playCount || 0);
          break;
        case 'year':
          cmp = (a.year || 0) - (b.year || 0);
          break;
        case 'duration':
          cmp = (a.duration || 0) - (b.duration || 0);
          break;
        case 'size':
          cmp = (a.size || 0) - (b.size || 0);
          break;
        default:
          cmp = (a.title || '').localeCompare(b.title || '', 'tr', { sensitivity: 'base' });
      }
      return isAsc ? cmp : -cmp;
    });
    return copy;
  }

  function rebuildSearchIndex() {
    const index = new Map();
    (state.librarySongs || []).forEach(song => {
      const key = String(song.id);
      index.set(key, {
        title: normalizeSearchText(song.title),
        artist: normalizeSearchText(song.artist),
        album: normalizeSearchText(song.album),
        folder: normalizeSearchText(song.folderName),
        genre: normalizeSearchText(song.genre)
      });
    });
    state.searchIndex = index;
  }

  function normalizeSearchText(value) {
    return String(value || '')
      .toLocaleLowerCase('tr-TR')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/ı/g, 'i')
      .replace(/[^\p{L}\p{N}]+/gu, ' ')
      .trim();
  }

  function editDistanceAtMost(a, b, maxDistance = 2) {
    if (a === b) return true;
    if (!a || !b || Math.abs(a.length - b.length) > maxDistance) return false;
    let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
    for (let i = 1; i <= a.length; i++) {
      const curr = new Array(b.length + 1);
      curr[0] = i;
      let rowMin = curr[0];
      for (let j = 1; j <= b.length; j++) {
        const cost = a[i - 1] === b[j - 1] ? 0 : 1;
        curr[j] = Math.min(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost);
        rowMin = Math.min(rowMin, curr[j]);
      }
      if (rowMin > maxDistance) return false;
      prev = curr;
    }
    return prev[b.length] <= maxDistance;
  }

  function scoreSearchMatch(normalizedQuery, song, precomputed = null) {
    const tokens = normalizedQuery.split(/\s+/).filter(Boolean);
    if (!tokens.length) return 0;

    const meta = precomputed || {
      title: normalizeSearchText(song.title),
      artist: normalizeSearchText(song.artist),
      album: normalizeSearchText(song.album),
      folder: normalizeSearchText(song.folderName),
      genre: normalizeSearchText(song.genre)
    };
    const fields = [
      ['title', meta.title],
      ['artist', meta.artist],
      ['album', meta.album],
      ['folder', meta.folder],
      ['genre', meta.genre]
    ];
    let score = 0;

    for (const [type, value] of fields) {
      if (!value) continue;
      const weight = type === 'title' ? 100 : type === 'artist' ? 90 : type === 'album' ? 75 : 55;
      const words = value.split(/\s+/).filter(Boolean);
      if (value === normalizedQuery) score = Math.max(score, 1000 + weight);
      else if (value.startsWith(normalizedQuery)) score = Math.max(score, 800 + weight);
      else if (value.includes(normalizedQuery)) score = Math.max(score, 600 + weight);

      for (const token of tokens) {
        let tokenScore = 0;
        for (const word of words) {
          if (word === token) { tokenScore = Math.max(tokenScore, 24); break; }
          if (word.startsWith(token)) tokenScore = Math.max(tokenScore, 16);
          else if (token.length >= 4 && word.includes(token)) tokenScore = Math.max(tokenScore, 11);
          else if (token.length >= 4 && editDistanceAtMost(word, token, token.length >= 7 ? 2 : 1)) tokenScore = Math.max(tokenScore, 7);
        }
        score += tokenScore > 0 ? tokenScore + weight : 0;
      }
    }
    return score;
  }

  function searchLibrarySongs(query, songs) {
    const normalizedQuery = normalizeSearchText(query);
    if (!normalizedQuery) return Array.isArray(songs) ? songs.slice() : [];
    const source = Array.isArray(songs) ? songs : [];
    return source
      .map((song, index) => ({
        song,
        index,
        score: scoreSearchMatch(normalizedQuery, song, state.searchIndex.get(String(song.id)) || null)
      }))
      .filter(item => item.score > 0)
      .sort((a, b) => b.score - a.score || a.index - b.index)
      .map(item => item.song);
  }

  function loadSongs(forceRefresh = false) {
    if (forceRefresh || !state.librarySongsLoaded) {
      state.librarySongs = Bridge.getSongs('title_asc') || [];
      rebuildSearchIndex();
      state.librarySongsLoaded = true;
    }

    let songs = Array.isArray(state.librarySongs) ? state.librarySongs.slice() : [];
    if (state.folderFilter) {
      const folderKey = normalizeSearchText(state.folderFilter.path || state.folderFilter.displayName);
      songs = songs.filter(song => {
        const path = normalizeSearchText(song.path || song.relativePath || '');
        const folder = normalizeSearchText(song.folderName || '');
        const pathMatches = folderKey && (path === folderKey || path.startsWith(folderKey + '/'));
        const nameMatches = folderKey && folder === folderKey;
        return Boolean(pathMatches || nameMatches);
      });
    }
    if (state.searchQuery.trim().length > 0) {
      songs = searchLibrarySongs(state.searchQuery, songs);
    }
    songs = sortSongsArray(songs, state.sortOrder);

    state.songs = songs;
    renderSongsList(state.songs);

    // Compute meta stats
    const totalMs = state.songs.reduce((acc, s) => acc + (s.duration || 0), 0);
    const totalMins = Math.round(totalMs / 60000);
    if (elements.songsCountText) {
      elements.songsCountText.textContent = `${state.songs.length} Şarkı`;
    }
    if (elements.songsDurationText) {
      elements.songsDurationText.textContent = `${totalMins} dakika`;
    }
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

  function loadEqualizer() {
    const eqData = Bridge.getEqualizerData();
    state.equalizerData = eqData;
    renderEqualizer(eqData);
  }

  // --- Rendering Functions ---
  function renderSongsList(songs) {
    if (!elements.songsList) return;
    if (state.isScanning && (!songs || songs.length === 0)) {
      elements.songsList.innerHTML = `
        <div style="padding: 14px 4px 10px 4px; display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 13.5px; font-weight: 600; color: var(--text-secondary);">Kütüphane taranıyor...</span>
        </div>
        <div class="skeleton-list">
          <div class="skeleton-song-row">
            <div class="skeleton-art shimmer"></div>
            <div class="skeleton-info">
              <div class="skeleton-line title shimmer"></div>
              <div class="skeleton-line sub shimmer"></div>
            </div>
          </div>
          <div class="skeleton-song-row">
            <div class="skeleton-art shimmer"></div>
            <div class="skeleton-info">
              <div class="skeleton-line title shimmer"></div>
              <div class="skeleton-line sub shimmer"></div>
            </div>
          </div>
          <div class="skeleton-song-row">
            <div class="skeleton-art shimmer"></div>
            <div class="skeleton-info">
              <div class="skeleton-line title shimmer"></div>
              <div class="skeleton-line sub shimmer"></div>
            </div>
          </div>
        </div>
      `;
      return;
    }
    if (!songs || songs.length === 0) {
      elements.songsList.innerHTML = `
        <div class="empty-state-card" style="text-align:center; padding: 40px 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px;">
          <div style="width: 56px; height: 56px; border-radius: 50%; background: var(--bg-surface-variant); border: 1px solid var(--border-color); display: flex; align-items: center; justify-content: center; color: var(--primary);">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
              <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
            </svg>
          </div>
          <div style="font-size: 16px; font-weight: 700; color: var(--text-primary);">Cihazda Müzik Bulunamadı</div>
          <div style="font-size: 13px; color: var(--text-secondary); max-width: 280px; line-height: 1.4;">
            Cihazınızda müzik aramak için taramayı başlatabilir veya klasör seçebilirsiniz.
          </div>
          <div style="display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; justify-content: center;">
            <button id="btnEmptyStateScan" class="btn-primary" style="padding: 9px 18px; font-size: 13.5px; border-radius: 12px; display: flex; align-items: center; gap: 8px; cursor: pointer;">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
              </svg>
              <span>Otomatik Tara</span>
            </button>
            <button id="btnEmptyStatePickFolder" class="btn-secondary" style="padding: 9px 18px; font-size: 13.5px; border-radius: 12px; display: flex; align-items: center; gap: 8px; cursor: pointer;">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
              </svg>
              <span>Klasör Seç (SAF)</span>
            </button>
          </div>
        </div>
      `;

      const scanBtn = document.getElementById('btnEmptyStateScan');
      if (scanBtn) {
        scanBtn.addEventListener('click', () => {
          showScanningState(true);
          showToast(tr('scanningMusic'));
          Bridge.scanLibrary();
        });
      }
      const pickBtn = document.getElementById('btnEmptyStatePickFolder');
      if (pickBtn) {
        pickBtn.addEventListener('click', () => {
          Bridge.pickSafFolder();
        });
      }
      return;
    }
    renderSongsIntoContainer(songs, elements.songsList, 'Cihazınızda müzik dosyası bulunamadı.');
  }

  function renderSongsIntoContainer(songs, container, emptyMsg) {
    if (!container) return;
    const token = (container._renderToken || 0) + 1;
    container._renderToken = token;
    if (container._renderObserver) {
      try { container._renderObserver.disconnect(); } catch (e) {}
      container._renderObserver = null;
    }
    if (container._renderFallbackHandler) {
      window.removeEventListener('scroll', container._renderFallbackHandler);
      container._renderFallbackHandler = null;
    }
    container.innerHTML = '';

    if (!songs || songs.length === 0) {
      container.innerHTML = `<div style="text-align:center; padding: 40px 16px; color: var(--text-tertiary); font-size: 14px;">${escapeHtml(emptyMsg)}</div>`;
      return;
    }

    const sourceSongs = Array.isArray(songs) ? songs : [];
    // Keep the DOM bounded for very large libraries. The player queue still retains
    // the complete source array, while only a small visible window is rendered.
    const pageSize = sourceSongs.length > 300 ? 72 : 120;
    let cursor = 0;

    const createCard = (song, idx) => {
      const card = document.createElement('div');
      card.className = 'song-card';
      card.setAttribute('data-id', String(song.id));
      if (state.currentSong && state.currentSong.id === song.id) card.classList.add('now-playing-item');

      const artUri = getSafeArtUri(song);
      const artWrap = document.createElement('div');
      artWrap.className = 'song-card-art';
      const placeholder = document.createElement('div');
      placeholder.className = 'art-icon';
      placeholder.style.display = artUri ? 'none' : 'flex';
      placeholder.innerHTML = '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>';
      if (artUri) {
        const img = document.createElement('img');
        img.src = artUri;
        img.alt = '';
        img.loading = 'lazy';
        img.decoding = 'async';
        img.addEventListener('error', () => {
          img.style.display = 'none';
          placeholder.style.display = 'flex';
        }, { once: true });
        artWrap.appendChild(img);
      }
      artWrap.appendChild(placeholder);
      card.appendChild(artWrap);

      const info = document.createElement('div');
      info.className = 'song-card-info';
      info.innerHTML = `<div class="song-card-title">${escapeHtml(song.title)}</div><div class="song-card-sub">${escapeHtml(song.artist)} • ${formatTime(song.duration)}</div>`;
      card.appendChild(info);

      const actions = document.createElement('div');
      actions.className = 'song-card-actions';
      actions.innerHTML = `
        <button class="fav-icon-btn ${song.isFavorite ? 'active' : ''}" data-id="${escapeHtml(song.id)}" title="Favori" aria-label="Favori">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
        </button>
        <button class="more-icon-btn" data-id="${escapeHtml(song.id)}" title="Seçenekler" aria-label="Seçenekler">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2z"/></svg>
        </button>`;
      card.appendChild(actions);

      card.addEventListener('click', (e) => {
        if (e.target.closest('.fav-icon-btn') || e.target.closest('.more-icon-btn')) return;
        playQueueSong(sourceSongs, idx);
      });

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
        const cached = (state.librarySongs || []).find(item => String(item.id) === String(song.id));
        if (cached) cached.isFavorite = newFav;
        showToast(newFav ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
      });

      card.querySelector('.more-icon-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        openContextMenu(song);
      });
      return card;
    };

    const sentinel = document.createElement('div');
    sentinel.className = 'list-render-sentinel';
    sentinel.setAttribute('aria-hidden', 'true');
    sentinel.style.cssText = 'height:1px;width:100%;pointer-events:none;';
    container.appendChild(sentinel);

    const appendPage = () => {
      if (container._renderToken !== token) return;
      const fragment = document.createDocumentFragment();
      const endCursor = Math.min(cursor + pageSize, sourceSongs.length);
      for (; cursor < endCursor; cursor++) {
        fragment.appendChild(createCard(sourceSongs[cursor], cursor));
      }
      container.insertBefore(fragment, sentinel);
      if (cursor >= sourceSongs.length) {
        sentinel.remove();
        if (container._renderObserver) {
          try { container._renderObserver.disconnect(); } catch (e) {}
          container._renderObserver = null;
        }
      }
    };

    if (typeof IntersectionObserver === 'function') {
      container._renderObserver = new IntersectionObserver((entries) => {
        if (entries.some(entry => entry.isIntersecting)) appendPage();
      }, { root: null, rootMargin: '700px 0px' });
      container._renderObserver.observe(sentinel);
      // Populate the first viewport immediately instead of waiting for observer scheduling.
      appendPage();
    } else {
      // Modern Android WebView supports IntersectionObserver; the fallback protects old
      // builds without causing an unbounded DOM on normal devices.
      appendPage();
      const fallback = () => {
        if (container._renderToken !== token || cursor >= sourceSongs.length) return;
        const rect = container.getBoundingClientRect();
        if (rect.bottom < window.innerHeight + 800) appendPage();
        if (cursor >= sourceSongs.length) {
          window.removeEventListener('scroll', fallback);
          container._renderFallbackHandler = null;
        }
      };
      container._renderFallbackHandler = fallback;
      window.addEventListener('scroll', fallback, { passive: true });
    }
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
    state.folderFilter = { path: folder.path || '', displayName: folder.displayName || '' };
    state.searchQuery = '';
    if (elements.searchInput) elements.searchInput.value = '';
    if (elements.clearSearchBtn) elements.clearSearchBtn.style.display = 'none';
    switchScreen('screenSongs');
    loadSongs();
  }

  function renderPlaylistsList(playlists) {
    const container = elements.playlistsList;
    container.innerHTML = '';
    if (!playlists || playlists.length === 0) {
      container.innerHTML = '<div style="text-align:center; padding: 20px; color: var(--text-tertiary);">Henüz çalma listesi oluşturmadınız.</div>';
      return;
    }

    const fragment = document.createDocumentFragment();
    playlists.forEach(pl => {
      const card = document.createElement('div');
      card.className = 'collection-row';
      card.innerHTML = `
        <div class="collection-row-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
            <path d="M4 5h12v2H4V5zm0 4h12v2H4V9zm0 4h8v2H4v-2zm11 0v5l5-2.5-5-2.5z"/>
          </svg>
        </div>
        <div class="collection-row-info">
          <div class="collection-row-title">${escapeHtml(pl.name)}</div>
          <div class="collection-row-sub">${Number(pl.songCount || 0)} şarkı</div>
        </div>
        <div class="collection-row-action" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M9 18l6-6-6-6"/></svg>
        </div>
      `;
      card.addEventListener('click', () => openPlaylistDetails(pl));
      fragment.appendChild(card);
    });
    container.appendChild(fragment);
  }

  function openPlaylistDetails(pl) {
    const songs = Bridge.getPlaylistSongs(pl.id);
    state.activeCollection = { type: 'playlist', id: pl.id, name: pl.name, songs: songs || [] };
    const title = document.getElementById('playlistDetailTitle');
    const subtitle = document.getElementById('playlistDetailSubtitle');
    if (title) title.textContent = pl.name;
    if (subtitle) subtitle.textContent = songCountLabel(songs.length);
    renderPlaylistDetailList(pl, songs);
    switchScreen('screenPlaylistDetail');
  }

  function renderPlaylistDetailList(playlist, songs) {
    const container = document.getElementById('playlistDetailSongs');
    if (!container) return;
    container.innerHTML = '';
    if (!songs.length) {
      container.innerHTML = `<div class="empty-state-inline">${escapeHtml(tr('emptyPlaylist'))}</div>`;
      return;
    }

    const fragment = document.createDocumentFragment();
    songs.forEach((song, index) => {
      const card = document.createElement('div');
      card.className = 'song-card playlist-detail-row';
      const artUri = getSafeArtUri(song);
      card.innerHTML = `
        <div class="song-card-art">
          <img src="${escapeHtml(artUri || '')}" alt="" loading="lazy" decoding="async" style="${artUri ? '' : 'display:none;'}">
          <div class="art-icon" style="${artUri ? 'display:none;' : 'display:flex;'}"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg></div>
        </div>
        <div class="song-card-info"><div class="song-card-title">${escapeHtml(song.title)}</div><div class="song-card-sub">${escapeHtml(song.artist)} • ${formatTime(song.duration)}</div></div>
        <div class="playlist-reorder-actions" role="group" aria-label="Şarkı sırasını değiştir">
          <button class="icon-action-btn playlist-move-up" type="button" title="${escapeHtml(tr('moveUp'))}" aria-label="${escapeHtml(tr('moveUp'))}" ${index === 0 ? 'disabled' : ''}>
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/></svg>
          </button>
          <button class="icon-action-btn playlist-move-down" type="button" title="${escapeHtml(tr('moveDown'))}" aria-label="${escapeHtml(tr('moveDown'))}" ${index === songs.length - 1 ? 'disabled' : ''}>
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/></svg>
          </button>
          <button class="icon-action-btn playlist-remove" type="button" title="${escapeHtml(tr('removeFromPlaylist'))}" aria-label="${escapeHtml(tr('removeFromPlaylist'))}">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M6 7h12v2H6V7zm2 3h8l-.7 10H8.7L8 10zm3-6h2l1 2h-4l1-2z"/></svg>
          </button>
        </div>`;
      card.addEventListener('click', (event) => {
        if (event.target.closest('.playlist-reorder-actions')) return;
        playQueueSong(songs, index);
      });
      card.querySelector('.playlist-move-up')?.addEventListener('click', (event) => {
        event.stopPropagation();
        movePlaylistSong(playlist, songs, index, index - 1);
      });
      card.querySelector('.playlist-move-down')?.addEventListener('click', (event) => {
        event.stopPropagation();
        movePlaylistSong(playlist, songs, index, index + 1);
      });
      card.querySelector('.playlist-remove')?.addEventListener('click', (event) => {
        event.stopPropagation();
        const removed = Bridge.removeSongFromPlaylist(playlist.id, song.id);
        if (removed === false) {
          showToast('Şarkı listeden çıkarılamadı');
          return;
        }
        const updated = songs.filter(item => Number(item.id) !== Number(song.id));
        state.activeCollection.songs = updated;
        const activePlaylist = (state.playlists || []).find(item => Number(item.id) === Number(playlist.id));
        if (activePlaylist) activePlaylist.songCount = updated.length;
        const subtitle = document.getElementById('playlistDetailSubtitle');
        if (subtitle) subtitle.textContent = songCountLabel(updated.length);
        renderPlaylistDetailList(playlist, updated);
        renderPlaylistsList(state.playlists || []);
        showToast('Şarkı listeden çıkarıldı');
      });
      card.querySelector('img')?.addEventListener('error', (event) => {
        event.currentTarget.style.display = 'none';
        event.currentTarget.nextElementSibling.style.display = 'flex';
      }, { once: true });
      fragment.appendChild(card);
    });
    container.appendChild(fragment);
  }

  function movePlaylistSong(playlist, songs, fromIndex, toIndex) {
    if (toIndex < 0 || toIndex >= songs.length || fromIndex === toIndex) return;
    const target = songs[fromIndex];
    const moved = Bridge.moveSongInPlaylist(playlist.id, target.id, toIndex);
    if (!moved) {
      showToast('Şarkı sırası değiştirilemedi');
      return;
    }
    const reordered = [...songs];
    const [item] = reordered.splice(fromIndex, 1);
    reordered.splice(toIndex, 0, item);
    state.activeCollection.songs = reordered;
    renderPlaylistDetailList(playlist, reordered);
  }

  function normalizeArtistKey(value) {
    return normalizeSearchText(value).replace(/\b(?:feat|ft|featuring)\b.*$/u, '').trim();
  }

  function persistPinnedArtists() {
    Bridge.setSetting('pinned_artists', JSON.stringify(state.pinnedArtists || []));
  }

  function isArtistPinned(artist) {
    const key = normalizeArtistKey(artist);
    return (state.pinnedArtists || []).some(item => normalizeArtistKey(item) === key);
  }

  function togglePinnedArtist(artist) {
    const existingIndex = (state.pinnedArtists || []).findIndex(item => normalizeArtistKey(item) === normalizeArtistKey(artist));
    if (existingIndex >= 0) {
      state.pinnedArtists.splice(existingIndex, 1);
      showToast(`"${artist}" sabitlemeden çıkarıldı`);
    } else {
      state.pinnedArtists.unshift(artist);
      showToast(`"${artist}" sanatçısı sabitlendi`);
    }
    persistPinnedArtists();
    renderArtistsList(state.artists);
  }

  function buildArtistItems(artists) {
    const grouped = new Map();
    (state.librarySongs || []).forEach(song => {
      const rawName = String(song.artist || '').trim();
      const key = normalizeArtistKey(rawName);
      if (!rawName || !key) return;
      const bucket = grouped.get(key) || { name: rawName, songCount: 0, variants: [] };
      bucket.songCount += 1;
      if (!bucket.variants.includes(rawName)) bucket.variants.push(rawName);
      if (bucket.name.toLowerCase().includes('bilinmeyen') || rawName.length < bucket.name.length) bucket.name = rawName;
      grouped.set(key, bucket);
    });

    (artists || []).forEach(rawArtist => {
      const rawName = String(rawArtist || '').trim();
      const key = normalizeArtistKey(rawName);
      if (!rawName || !key || grouped.has(key)) return;
      grouped.set(key, { name: rawName, songCount: 0, variants: [rawName] });
    });

    const normalizedPins = new Map((state.pinnedArtists || []).map((name, index) => [normalizeArtistKey(name), index]));
    return [...grouped.entries()].map(([key, item]) => ({
      name: item.name,
      songCount: item.songCount,
      variants: item.variants,
      pinned: normalizedPins.has(key),
      pinIndex: normalizedPins.get(key) ?? Number.MAX_SAFE_INTEGER
    })).sort((a, b) => {
      if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
      if (a.pinned && b.pinned && a.pinIndex !== b.pinIndex) return a.pinIndex - b.pinIndex;
      if (b.songCount !== a.songCount) return b.songCount - a.songCount;
      return a.name.localeCompare(b.name, 'tr', { sensitivity: 'base' });
    });
  }

  function renderArtistsList(artists) {
    const container = elements.artistsList;
    container.innerHTML = '';
    if (!artists || artists.length === 0) {
      container.innerHTML = '<div style="text-align:center; padding: 20px; color: var(--text-tertiary);">Sanatçı bulunamadı.</div>';
      return;
    }

    const fragment = document.createDocumentFragment();
    artists.forEach(item => {
      const artist = typeof item === 'string' ? { name: item, songCount: 0, pinned: isArtistPinned(item) } : item;
      const card = document.createElement('div');
      card.className = `collection-row ${artist.pinned ? 'is-pinned' : ''}`;
      card.innerHTML = `
        <div class="collection-row-icon artist-row-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
        </div>
        <div class="collection-row-info">
          <div class="collection-row-title">${escapeHtml(artist.name)}</div>
          <div class="collection-row-sub">${Number(artist.songCount || 0)} şarkı</div>
        </div>
        <button class="collection-pin-btn ${artist.pinned ? 'active' : ''}" type="button" title="${artist.pinned ? 'Sabitlemeyi kaldır' : 'Sanatçıyı sabitle'}" aria-label="${artist.pinned ? 'Sabitlemeyi kaldır' : 'Sanatçıyı sabitle'}">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="currentColor" aria-hidden="true">
            <path d="M16 9V4h1V2H7v2h1v5l-2 3v2h5v8h2v-8h5v-2l-2-3z"/>
          </svg>
        </button>
        <div class="collection-row-action" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M9 18l6-6-6-6"/></svg>
        </div>
      `;
      card.addEventListener('click', () => openArtistDetails(artist.name));
      const pinBtn = card.querySelector('.collection-pin-btn');
      pinBtn.addEventListener('click', (event) => {
        event.stopPropagation();
        togglePinnedArtist(artist.name);
      });
      fragment.appendChild(card);
    });
    container.appendChild(fragment);
  }

  function renderAlbumsList(albums) {
    const container = elements.albumsList;
    container.innerHTML = '';
    if (!albums || albums.length === 0) {
      container.innerHTML = '<div style="grid-column:1/-1;text-align:center; padding: 20px; color: var(--text-tertiary);">Albüm bulunamadı.</div>';
      return;
    }

    const fragment = document.createDocumentFragment();
    albums.forEach(album => {
      const item = typeof album === 'string' ? { name: album, artist: '', count: Number(state.albumCounts.get(normalizeSearchText(album)) || 0), artUri: '' } : album;
      if (item && item.artist && !Number.isFinite(Number(item.count))) item.count = 0;
      if (item && item.artist && Number(item.count) === 0) {
        item.count = Number(state.albumCounts.get(normalizeSearchText(`${item.name} ${item.artist}`)) || 0);
      }
      const card = document.createElement('div');
      card.className = 'grid-card';
      const artUri = item.artUri ? getSafeArtUri({ id: item.id || item.artSongId, albumArtUri: item.artUri, dateModified: item.dateModified }) : '';
      card.innerHTML = `
        <div class="grid-card-art-wrap">
          <img class="grid-card-art" src="${escapeHtml(artUri)}" alt="" loading="lazy" decoding="async" referrerpolicy="no-referrer" />
          <div class="grid-card-icon ${artUri ? 'fallback-art-icon' : ''}" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z"/>
            </svg>
          </div>
        </div>
        <div class="grid-card-title">${escapeHtml(item.name)}</div>
        <div class="grid-card-sub">${escapeHtml(item.artist || `${Number(item.count || 0)} şarkı`)}</div>
        ${item.artist ? `<div class="grid-card-meta">${Number(item.count || 0)} şarkı</div>` : ''}
      `;
      const img = card.querySelector('.grid-card-art');
      const fallback = card.querySelector('.fallback-art-icon');
      if (img) {
        if (!artUri) img.style.display = 'none';
        img.addEventListener('error', () => {
          img.style.display = 'none';
          if (fallback) fallback.style.display = 'flex';
        }, { once: true });
        if (artUri) {
          img.addEventListener('load', () => {
            if (fallback) fallback.style.display = 'none';
          }, { once: true });
        }
      }
      card.addEventListener('click', () => openAlbumDetails(item.name, null, item.artist));
      fragment.appendChild(card);
    });
    container.appendChild(fragment);
  }

  function sortCollectionSongs(songs, type) {
    const list = Array.isArray(songs) ? [...songs] : [];
    return list.sort((a, b) => {
      if (type === 'artist') {
        const albumCmp = normalizeSearchText(a.album).localeCompare(normalizeSearchText(b.album), 'tr', { sensitivity: 'base' });
        if (albumCmp !== 0) return albumCmp;
      }
      const discCmp = Number(a.discNumber || 0) - Number(b.discNumber || 0);
      if (discCmp !== 0) return discCmp;
      const trackCmp = Number(a.trackNumber || 0) - Number(b.trackNumber || 0);
      if (trackCmp !== 0) return trackCmp;
      return String(a.title || '').localeCompare(String(b.title || ''), 'tr', { sensitivity: 'base' });
    });
  }

  function openArtistDetails(artist, songsOverride = null) {
    const key = normalizeArtistKey(artist);
    const localSongs = (state.librarySongs || []).filter(song => normalizeArtistKey(song.artist) === key);
    const songs = sortCollectionSongs(songsOverride || localSongs, 'artist');
    state.activeCollection = { type: 'artist', name: artist, songs: songs || [] };
    const title = document.getElementById('artistDetailTitle');
    const subtitle = document.getElementById('artistDetailSubtitle');
    if (title) title.textContent = artist;
    if (subtitle) subtitle.textContent = `${(songs || []).length} şarkı`;
    const list = document.getElementById('artistDetailSongs');
    renderSongsIntoContainer(songs || [], list, 'Bu sanatçıya ait şarkı bulunamadı.');
    switchScreen('screenArtistDetail');
  }

  function openAlbumDetails(album, songsOverride = null, artist = '') {
    const albumKey = normalizeSearchText(album);
    const artistKey = normalizeArtistKey(artist);
    const localSongs = (state.librarySongs || []).filter(song => {
      if (normalizeSearchText(song.album) !== albumKey) return false;
      return !artistKey || normalizeArtistKey(song.artist) === artistKey;
    });
    const songs = sortCollectionSongs(songsOverride || localSongs, 'album');
    state.activeCollection = { type: 'album', name: album, artist, songs: songs || [] };
    const title = document.getElementById('albumDetailTitle');
    const subtitle = document.getElementById('albumDetailSubtitle');
    if (title) title.textContent = album;
    if (subtitle) subtitle.textContent = `${artist ? `${artist} • ` : ''}${(songs || []).length} şarkı`;
    const list = document.getElementById('albumDetailSongs');
    renderSongsIntoContainer(songs || [], list, 'Bu albümde şarkı bulunamadı.');
    switchScreen('screenAlbumDetail');
  }

  function loadArtistsAndAlbums() {
    const rawArtists = [];
    const artistKeys = new Set();
    const albumMap = new Map();
    state.albumCounts = new Map();
    (state.librarySongs || []).forEach(song => {
      const artist = String(song.artist || '').trim();
      const artistKey = normalizeArtistKey(artist);
      if (artist && artistKey && !artistKeys.has(artistKey)) {
        artistKeys.add(artistKey);
        rawArtists.push(artist);
      }

      const albumName = String(song.album || '').trim();
      const albumArtist = String(song.artist || '').trim();
      const albumKey = normalizeSearchText(`${albumName} ${albumArtist}`);
      if (albumName && albumKey) {
        const existing = albumMap.get(albumKey);
        if (existing) {
          existing.count += 1;
        } else {
          albumMap.set(albumKey, {
            name: albumName,
            artist: albumArtist,
            count: 1,
            artUri: song.albumArtUri || '',
            artSongId: song.id,
            dateModified: song.dateModified || 0
          });
        }
      }
      const countKey = normalizeSearchText(`${albumName} ${albumArtist}`);
      if (countKey) state.albumCounts.set(countKey, (state.albumCounts.get(countKey) || 0) + 1);
    });
    state.artists = buildArtistItems(rawArtists);
    state.albums = [...albumMap.values()].sort((a, b) => a.name.localeCompare(b.name, 'tr', { sensitivity: 'base' }) || a.artist.localeCompare(b.artist, 'tr', { sensitivity: 'base' }));
    renderArtistsList(state.artists);
    renderAlbumsList(state.albums);
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
    if (!song) return;

    state.currentSong = song;
    state.position = 0;
    state.duration = song.duration > 0 ? song.duration : 0;
    state.isPlaying = true;

    // Optimistic immediate UI update
    updateUIWithCurrentSong(song);
    updatePlayPauseIcons(true);
    updateProgressDisplay();

    Bridge.playSong(song.id, queue, startIndex);
  }

  function updateUIWithCurrentSong(song) {
    if (!song) return;

    const safeTitle = song.title && song.title.trim() ? song.title.trim() : 'Bilinmeyen Şarkı';
    const safeArtist = song.artist && song.artist.trim() ? song.artist.trim() : 'Bilinmeyen Sanatçı';
    const safeAlbum = song.album && song.album.trim() && song.album.toLowerCase() !== '<unknown>' && song.album.toLowerCase() !== 'null' ? song.album.trim() : tr('unknownAlbum');

    // Immediately set duration and total time
    if (song.duration && song.duration > 0) {
      state.duration = song.duration;
      if (elements.npTotalTime) {
        elements.npTotalTime.textContent = formatTime(song.duration);
      }
    }

    // Mini Player
    if (elements.miniPlayer) elements.miniPlayer.style.display = 'flex';
    if (elements.miniTitle) elements.miniTitle.textContent = safeTitle;
    if (elements.miniArtist) elements.miniArtist.textContent = safeArtist;
    updateArtworkElement(elements.miniArt, elements.miniArtPlaceholder, song);

    // Now Playing Screen
    if (elements.npTitle) elements.npTitle.textContent = safeTitle;
    if (elements.npArtist) elements.npArtist.textContent = `${safeArtist} • ${safeAlbum}`;
    if (elements.npHeaderAlbum) elements.npHeaderAlbum.textContent = safeAlbum;
    updateArtworkElement(elements.npArtwork, elements.npArtPlaceholder, song);
    updateNowPlayingBlurredBackground(song);

    updateNowPlayingFavoriteIcon(song.isFavorite);

    // Active item highlight in songs list
    document.querySelectorAll('.song-card').forEach(card => {
      const cardId = card.getAttribute('data-id');
      if (cardId && String(cardId) === String(song.id)) {
        card.classList.add('now-playing-item');
      } else {
        card.classList.remove('now-playing-item');
      }
    });

    if (state.isLyricsTabActive) {
      loadLyricsForCurrentSong(true);
    } else {
      state.currentLyricsSongId = null;
      state.lyrics = [];
    }

    updateProgressDisplay();
  }

  function updateProgressDisplay() {
    const dur = state.duration > 0 ? state.duration : (state.currentSong?.duration || 0);
    const pos = Math.max(0, Math.min(state.position || 0, dur > 0 ? dur : Infinity));
    const ratio = dur > 0 ? Math.min(1, Math.max(0, pos / dur)) : 0;
    const percent = (ratio * 100).toFixed(2);

    if (elements.npSeekSlider && !state.isSeeking) {
      elements.npSeekSlider.value = Math.round(ratio * 1000);
      elements.npSeekSlider.style.background = `linear-gradient(to right, var(--primary) 0%, var(--primary) ${percent}%, rgba(255, 255, 255, 0.12) ${percent}%, rgba(255, 255, 255, 0.12) 100%)`;
    }

    if (elements.miniProgressFill) {
      elements.miniProgressFill.style.width = `${percent}%`;
    }

    if (elements.npCurrentTime) {
      elements.npCurrentTime.textContent = formatTime(pos);
    }

    if (elements.npTotalTime && dur > 0) {
      elements.npTotalTime.textContent = formatTime(dur);
    }

    // Auto-scroll synchronized LRC lyrics during playback
    if (state.isLyricsTabActive && state.lyrics && state.lyrics.length > 0) {
      syncLyricsScroll(pos);
    }
  }

  function applyPlaybackState(stateObj) {
    if (!stateObj) return;

    state.isPlaying = !!stateObj.isPlaying;
    state.isShuffle = !!stateObj.isShuffle;
    state.repeatMode = stateObj.repeatMode !== undefined ? stateObj.repeatMode : state.repeatMode;
    state.playbackSpeed = stateObj.playbackSpeed || 1.0;
    state.sleepTimerRemaining = stateObj.sleepTimerRemaining || 0;

    if (stateObj.queue && Array.isArray(stateObj.queue)) {
      state.queue = stateObj.queue;
    }
    if (typeof stateObj.currentIndex === 'number' && stateObj.currentIndex >= 0) {
      state.currentQueueIndex = stateObj.currentIndex;
    }

    if (stateObj.hasSong && stateObj.currentSong) {
      const incomingSong = stateObj.currentSong;
      const previousSong = state.currentSong;
      const songChanged = !previousSong || String(previousSong.id) !== String(incomingSong.id);

      state.currentSong = incomingSong;

      if (songChanged) {
        updateUIWithCurrentSong(incomingSong);
      } else {
        // Avoid rebuilding artwork, blur, lyrics and the whole song list on
        // every playback tick. Favorite state can still change in-place.
        updateNowPlayingFavoriteIcon(incomingSong.isFavorite);
      }
    }

    const dur = (stateObj.duration && stateObj.duration > 0)
      ? stateObj.duration
      : ((state.currentSong && state.currentSong.duration > 0) ? state.currentSong.duration : 0);

    if (dur > 0) {
      state.duration = dur;
      if (elements.npTotalTime) elements.npTotalTime.textContent = formatTime(dur);
    }

    if (!state.isSeeking) {
      state.position = Math.max(0, stateObj.position || 0);
      updateProgressDisplay();
    }

    updatePlayPauseIcons(state.isPlaying);
    updateRepeatModeUI(state.repeatMode);

    if (elements.npShuffleBtn) elements.npShuffleBtn.classList.toggle('active', state.isShuffle);
    if (elements.npSpeedText) elements.npSpeedText.textContent = `${state.playbackSpeed.toFixed(1)}x`;

    if (elements.sleepTimerBadge) {
      if (state.sleepTimerRemaining > 0) {
        elements.sleepTimerBadge.style.display = 'inline-flex';
        const m = Math.floor(state.sleepTimerRemaining / 60);
        const s = state.sleepTimerRemaining % 60;
        if (elements.sleepTimerBadgeText) {
          elements.sleepTimerBadgeText.textContent = `Uyku: ${m < 10 ? '0' + m : m}:${s < 10 ? '0' + s : s}`;
        }
      } else {
        elements.sleepTimerBadge.style.display = 'none';
      }
    }
  }

  function updateNowPlayingFavoriteIcon(isFav) {
    if (elements.npFavoriteBtn) elements.npFavoriteBtn.classList.toggle('active', !!isFav);
    if (elements.npFavQuickBtn) elements.npFavQuickBtn.classList.toggle('active', !!isFav);
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

  let lastNativePlaybackUpdateAt = 0;

  function updatePlaybackTick() {
    // Native MediaSession updates are authoritative. Only poll as a slow
    // fallback when a native callback has not arrived recently.
    if (Date.now() - lastNativePlaybackUpdateAt < 2500) return;
    const currentState = Bridge.getCurrentState();
    if (currentState) {
      applyPlaybackState(currentState);
    }
  }

  function updatePlayPauseIcons(isPlaying) {
    const playPath = "M8 5v14l11-7z";
    const pausePath = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";

    if (elements.miniPlayIcon) {
      const p = elements.miniPlayIcon.querySelector('path');
      if (p) p.setAttribute('d', isPlaying ? pausePath : playPath);
    }
    if (elements.npPlayIcon) {
      const p = elements.npPlayIcon.querySelector('path');
      if (p) p.setAttribute('d', isPlaying ? pausePath : playPath);
    }

    // Edge lighting kept strictly disabled to eliminate lag & corner glow
    if (elements.screenEdgeLighting) {
      elements.screenEdgeLighting.classList.remove('active', 'is-playing');
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
    updateArtworkElement(elements.contextArt, null, song);

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
          <button class="icon-btn remove-queue-btn" data-index="${idx}" title="Kuyruktan çıkar" aria-label="Kuyruktan çıkar" style="width:32px; height:32px;">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.41 4.3 19.71 2.89 18.3 9.17 12 2.89 5.7 4.3 4.29l6.29 6.3 6.29-6.3z"/></svg>
          </button>
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
      <div class="meta-row"><span class="meta-label">${escapeHtml(tr('songTitleLabel'))}:</span><span class="meta-value">${escapeHtml(song.title)}</span></div>
      <div class="meta-row"><span class="meta-label">${escapeHtml(tr('artistLabel'))}:</span><span class="meta-value">${escapeHtml(song.artist)}</span></div>
      <div class="meta-row"><span class="meta-label">${escapeHtml(tr('albumLabel'))}:</span><span class="meta-value">${escapeHtml(song.album)}</span></div>
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

  // --- Screen Navigation (Screenshot 3) ---
  function switchScreen(screenId) {
    state.activeScreen = screenId;
    document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));

    // Sync top navigation tabs
    document.querySelectorAll('.top-tab-btn').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-screen') === screenId);
    });

    // Sync side drawer items
    document.querySelectorAll('.drawer-item').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-screen') === screenId);
    });

    const screenEl = document.getElementById(screenId);
    if (screenEl) screenEl.classList.add('active');

    const navBtn = document.querySelector(`.nav-item[data-screen="${screenId}"]`);
    if (navBtn) navBtn.classList.add('active');

    if (screenId === 'screenSongs') {
      loadSongs();
    } else if (screenId === 'screenPlaylists') {
      loadPlaylists();
    } else if (screenId === 'screenFolders') {
      loadFolders();
    } else if (screenId === 'screenAlbums') {
      loadArtistsAndAlbums();
    } else if (screenId === 'screenArtists') {
      loadArtistsAndAlbums();
    } else if (screenId === 'screenArtistDetail') {
      if (state.activeCollection && state.activeCollection.type === 'artist') {
        const collection = state.activeCollection;
        const title = document.getElementById('artistDetailTitle');
        const subtitle = document.getElementById('artistDetailSubtitle');
        if (title) title.textContent = collection.name;
        if (subtitle) subtitle.textContent = `${collection.songs.length} şarkı`;
        renderSongsIntoContainer(collection.songs, document.getElementById('artistDetailSongs'), 'Bu sanatçıya ait şarkı bulunamadı.');
      }
    } else if (screenId === 'screenAlbumDetail') {
      if (state.activeCollection && state.activeCollection.type === 'album') {
        const collection = state.activeCollection;
        const title = document.getElementById('albumDetailTitle');
        const subtitle = document.getElementById('albumDetailSubtitle');
        if (title) title.textContent = collection.name;
        if (subtitle) subtitle.textContent = `${collection.songs.length} şarkı`;
        renderSongsIntoContainer(collection.songs, document.getElementById('albumDetailSongs'), 'Bu albümde şarkı bulunamadı.');
      }
    } else if (screenId === 'screenPlaylistDetail') {
      if (state.activeCollection && state.activeCollection.type === 'playlist') {
        const collection = state.activeCollection;
        const title = document.getElementById('playlistDetailTitle');
        const subtitle = document.getElementById('playlistDetailSubtitle');
        if (title) title.textContent = collection.name;
        if (subtitle) subtitle.textContent = `${collection.songs.length} şarkı`;
        const playlist = (state.playlists || []).find(item => Number(item.id) === Number(collection.id));
        if (playlist) renderPlaylistDetailList(playlist, collection.songs);
      }
    } else if (screenId === 'screenEqualizer') {
      loadEqualizer();
    }
  }

  // --- Summary Modal ("Özetini Görüntüle" - Screenshot 3) ---
  function openSummaryModal() {
    if (!elements.summaryModal) return;
    const totalCount = state.songs.length;
    const totalMs = state.songs.reduce((acc, s) => acc + (s.duration || 0), 0);
    const totalMinutes = Math.round(totalMs / 60000);
    const totalHours = (totalMs / 3600000).toFixed(1);

    const sumSongs = document.getElementById('sumTotalSongs') || elements.summarySongsCount;
    const sumDur = document.getElementById('sumTotalDuration') || elements.summaryDurationText;
    const sumArtists = document.getElementById('sumTotalArtists') || elements.summaryArtistsCount;
    const sumAlbums = document.getElementById('sumTotalAlbums') || elements.summaryAlbumsCount;
    const sumFolders = document.getElementById('sumTotalFolders') || elements.summaryFoldersCount;
    const sumFavs = document.getElementById('sumTotalFavs') || elements.summaryFavoritesCount;

    if (sumSongs) sumSongs.textContent = totalCount;
    if (sumDur) sumDur.textContent = totalMinutes > 60 ? `${totalHours} saat` : `${totalMinutes} dk`;

    // Summary may be opened before the corresponding collection screen has ever been
    // visited. Calculate the small aggregates lazily instead of loading every hidden view
    // during startup/resume.
    loadArtistsAndAlbums();
    state.folders = Bridge.getFolders() || [];
    state.favorites = Bridge.getFavorites() || [];

    if (sumArtists) sumArtists.textContent = state.artists.length || 0;
    if (sumAlbums) sumAlbums.textContent = state.albums.length || 0;
    if (sumFolders) sumFolders.textContent = state.folders.length || 0;
    if (sumFavs) sumFavs.textContent = state.favorites.length || 0;

    elements.summaryModal.style.display = 'flex';
  }

  // --- Sort Bottom Sheet (Screenshot 4) ---
  function openSortModal() {
    if (!elements.sortModal) return;
    const parts = (state.sortOrder || 'date_added_desc').split('_');
    const dir = parts[parts.length - 1];
    const field = parts.slice(0, parts.length - 1).join('_');

    const fieldRadio = elements.sortModal.querySelector(`input[name="sortField"][value="${field}"]`);
    if (fieldRadio) fieldRadio.checked = true;
    const dirRadio = elements.sortModal.querySelector(`input[name="sortDirection"][value="${dir}"]`);
    if (dirRadio) dirRadio.checked = true;

    elements.sortModal.style.display = 'flex';
  }

  function applySortFromModal() {
    if (!elements.sortModal) return;
    const field = elements.sortModal.querySelector('input[name="sortField"]:checked')?.value || 'date_added';
    const direction = elements.sortModal.querySelector('input[name="sortDirection"]:checked')?.value || 'desc';
    state.sortOrder = `${field}_${direction}`;
    Bridge.setSetting('sort_order', state.sortOrder);
    if (elements.sortSelect) elements.sortSelect.value = state.sortOrder;
    loadSongs();
    
    const fieldNames = {
      title: 'Şarkı adına göre',
      artist: 'Sanatçıya göre',
      album: 'Albüme göre',
      folder: 'Klasöre göre',
      date_added: 'Eklenme tarihine göre',
      play_count: 'Çalma sayısına göre',
      year: 'Yıla göre',
      duration: 'Süreye göre',
      size: 'Boyuta göre'
    };
    const dirNames = {
      asc: '(Eskiden yeniye / A-Z)',
      desc: '(Yeniden eskiye / Z-A)'
    };
    const fName = fieldNames[field] || 'Sıralama';
    const dName = dirNames[direction] || '';
    showToast(`${fName} sıralandı ${dName}`);
    elements.sortModal.style.display = 'none';
  }

  // --- Dedicated Search Screen (Screenshot 6) ---
  function openSearchScreen() {
    const screen = elements.searchScreen || document.getElementById('fullSearchScreen');
    if (!screen) return;
    if (!state.playlists.length) state.playlists = Bridge.getPlaylists() || [];
    screen.style.display = 'flex';
    state.searchFilter = 'all';
    const input = elements.fullSearchInput || document.getElementById('dedicatedSearchInput');
    if (input) {
      input.value = '';
      setTimeout(() => input.focus(), 150);
    }
    renderFullSearchResults('');
  }

  function closeSearchScreen() {
    const screen = elements.searchScreen || document.getElementById('fullSearchScreen');
    if (screen) screen.style.display = 'none';
  }

  function renderFullSearchResults(query) {
    const resultsContainer = elements.fullSearchResults || document.getElementById('searchResultsList');
    if (!resultsContainer) return;
    resultsContainer.innerHTML = '';
    const q = normalizeSearchText(query);

    if (!q) {
      resultsContainer.innerHTML = '<div style="text-align:center; padding: 40px 16px; color: var(--text-tertiary); font-size: 14px;">Aramak için bir şarkı, sanatçı veya albüm adı yazın.</div>';
      return;
    }

    if (!state.librarySongsLoaded) {
      state.librarySongs = Bridge.getSongs('title_asc') || [];
      rebuildSearchIndex();
      state.librarySongsLoaded = true;
    }

    const sourceSongs = state.librarySongs;
    const matchingSongs = searchLibrarySongs(q, sourceSongs);
    const filter = state.searchFilter || 'all';
    const sections = [];

    if (filter === 'all' || filter === 'songs') {
      if (matchingSongs.length) sections.push({
        title: 'Şarkılar',
        items: matchingSongs.slice(0, 100).map((song, index) => ({ type: 'song', song, index }))
      });
    }

    const groupedArtists = new Map();
    matchingSongs.forEach(song => {
      const name = String(song.artist || '').trim();
      const key = normalizeArtistKey(name);
      if (!name || !key) return;
      const bucket = groupedArtists.get(key) || { name, count: 0 };
      bucket.count += 1;
      groupedArtists.set(key, bucket);
    });
    if (filter === 'all' || filter === 'artists') {
      const artists = [...groupedArtists.values()].sort((a,b) => b.count - a.count || a.name.localeCompare(b.name, 'tr', { sensitivity: 'base' }));
      if (artists.length) sections.push({ title: 'Sanatçılar', items: artists.slice(0, 50).map(item => ({ type: 'artist', name: item.name, count: item.count })) });
    }

    const groupedAlbums = new Map();
    matchingSongs.forEach(song => {
      const name = String(song.album || '').trim();
      const key = normalizeSearchText(`${name} ${song.artist || ''}`);
      if (name && key && !groupedAlbums.has(key)) groupedAlbums.set(key, { name, artist: String(song.artist || '').trim() });
    });
    if (filter === 'all' || filter === 'albums') {
      const albums = [...groupedAlbums.values()].sort((a,b) => a.name.localeCompare(b.name, 'tr', { sensitivity: 'base' }));
      if (albums.length) sections.push({ title: 'Albümler', items: albums.slice(0, 50).map(album => ({ type: 'album', ...album })) });
    }

    if (filter === 'all' || filter === 'playlists') {
      const playlists = (state.playlists || []).filter(pl => normalizeSearchText(pl.name).includes(q));
      if (playlists.length) sections.push({ title: 'Çalma Listeleri', items: playlists.slice(0, 50).map(pl => ({ type: 'playlist', playlist: pl })) });
    }

    if (!sections.length) {
      resultsContainer.innerHTML = '<div style="text-align:center; padding: 40px 16px; color: var(--text-tertiary); font-size: 14px;">Eşleşen sonuç bulunamadı.</div>';
      return;
    }

    sections.forEach(section => {
      const heading = document.createElement('div');
      heading.className = 'search-section-title';
      heading.textContent = section.title;
      resultsContainer.appendChild(heading);

      const container = document.createElement('div');
      container.className = 'songs-list';

      section.items.forEach(item => {
        const card = document.createElement('div');
        card.className = 'song-card';

        if (item.type === 'song') {
          const song = item.song;
          const artUri = getSafeArtUri(song);
          card.innerHTML = `
            <div class="song-card-art">
              ${artUri ? `<img src="${escapeHtml(artUri)}" alt="art">` : ''}
              <div class="art-icon" style="${artUri ? 'display:none;' : ''}">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
              </div>
            </div>
            <div class="song-card-info"><div class="song-card-title">${escapeHtml(song.title)}</div><div class="song-card-sub">${escapeHtml(song.artist)} • ${formatTime(song.duration)}</div></div>`;
          card.addEventListener('click', () => playQueueSong(matchingSongs, matchingSongs.indexOf(song)));
        } else if (item.type === 'artist') {
          card.innerHTML = `<div class="song-card-art"><div class="art-icon"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg></div></div><div class="song-card-info"><div class="song-card-title">${escapeHtml(item.name)}</div><div class="song-card-sub">Sanatçı${item.count ? ` • ${item.count} şarkı` : ''}</div></div>`;
          card.addEventListener('click', () => {
            const songs = state.librarySongs.filter(song => normalizeArtistKey(song.artist) === normalizeArtistKey(item.name));
            closeSearchScreen();
            openArtistDetails(item.name, songs);
          });
        } else if (item.type === 'album') {
          card.innerHTML = `<div class="song-card-art"><div class="art-icon"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z"/></svg></div></div><div class="song-card-info"><div class="song-card-title">${escapeHtml(item.name)}</div><div class="song-card-sub">${escapeHtml(item.artist || 'Albüm')}</div></div>`;
          card.addEventListener('click', () => {
            const songs = state.librarySongs.filter(song => normalizeSearchText(song.album) === normalizeSearchText(item.name) && (!item.artist || normalizeSearchText(song.artist) === normalizeSearchText(item.artist)));
            closeSearchScreen();
            openAlbumDetails(item.name, songs, item.artist);
          });
        } else {
          const pl = item.playlist;
          card.innerHTML = `<div class="song-card-art"><div class="art-icon"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M4 5h12v2H4V5zm0 4h12v2H4V9zm0 4h8v2H4v-2zm11 0v5l5-2.5-5-2.5z"/></svg></div></div><div class="song-card-info"><div class="song-card-title">${escapeHtml(pl.name)}</div><div class="song-card-sub">${Number(pl.songCount || 0)} şarkı</div></div>`;
          card.addEventListener('click', () => {
            const songs = Bridge.getPlaylistSongs(pl.id);
            closeSearchScreen();
            if (songs.length) playQueueSong(songs, 0);
          });
        }
        container.appendChild(card);
      });
      resultsContainer.appendChild(container);
    });
  }

  // --- Side Navigation Drawer ---
  function openSideDrawer() {
    const backdrop = elements.sideDrawerBackdrop || document.getElementById('drawerBackdrop');
    if (backdrop) {
      backdrop.style.display = 'flex';
      const countEl = document.getElementById('drawerSongCount');
      if (countEl) countEl.textContent = `${state.songs.length} parça hazır`;
    }
  }

  function closeSideDrawer() {
    const backdrop = elements.sideDrawerBackdrop || document.getElementById('drawerBackdrop');
    if (backdrop) backdrop.style.display = 'none';
  }

  // --- Event Binding ---
  function setNowPlayingOpen(isOpen) {
    if (!elements.nowPlayingModal) return;
    elements.nowPlayingModal.classList.toggle('active', isOpen);
    document.documentElement.classList.toggle('now-playing-open', isOpen);
  }

  function bindEvents() {
    // Safe event attachment helper with strict deduplication
    function on(target, event, handler) {
      if (!target) return;
      const el = typeof target === 'string' ? document.getElementById(target) : target;
      if (!el || typeof el.addEventListener !== 'function') return;
      const key = '__bound_' + event;
      if (el[key]) return;
      el[key] = true;
      el.addEventListener(event, handler);
    }

    // Attach graceful image error fallbacks
    if (elements.miniArt) {
      elements.miniArt.onerror = () => {
        elements.miniArt.style.display = 'none';
        if (elements.miniArtPlaceholder) elements.miniArtPlaceholder.style.display = 'flex';
      };
    }
    if (elements.npArtwork) {
      elements.npArtwork.onerror = () => {
        elements.npArtwork.style.display = 'none';
        if (elements.npArtPlaceholder) elements.npArtPlaceholder.style.display = 'flex';
      };
    }
    if (elements.contextArt) {
      elements.contextArt.onerror = () => {
        elements.contextArt.style.display = 'none';
      };
    }

    // Theme toggle
    on(elements.themeToggleBtn, 'click', toggleTheme);

    // Side Drawer Open / Close
    on(elements.headerMenuBtn, 'click', openSideDrawer);
    on('drawerToggleBtn', 'click', openSideDrawer);
    on(elements.closeDrawerBtn, 'click', closeSideDrawer);
    on(elements.sideDrawerBackdrop, 'click', (e) => {
      if (e.target === elements.sideDrawerBackdrop) closeSideDrawer();
    });

    // Drawer Navigation Items
    document.querySelectorAll('.drawer-item').forEach(btn => {
      btn.addEventListener('click', () => {
        closeSideDrawer();
        const screenId = btn.getAttribute('data-screen');
        if (screenId) {
          document.querySelectorAll('.drawer-item').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          switchScreen(screenId);
        }
      });
    });

    on('drawerEqBtn', 'click', () => {
      closeSideDrawer();
      switchScreen('screenEqualizer');
    });

    on('drawerSleepBtn', 'click', () => {
      closeSideDrawer();
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'flex';
    });

    on('drawerRescanBtn', 'click', () => {
      closeSideDrawer();
      if (elements.scanIcon) elements.scanIcon.style.animation = 'spin 0.8s linear infinite';
      Bridge.scanLibrary();
      showToast(tr('scanningMusic'));
    });

    on('drawerThemeBtn', 'click', () => {
      closeSideDrawer();
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
    });

    on('drawerSettingsBtn', 'click', () => {
      closeSideDrawer();
      if (elements.settingsModal) elements.settingsModal.style.display = 'flex';
    });

    on('drawerLangBtn', 'click', () => {
      closeSideDrawer();
      if (elements.settingsModal) {
        elements.settingsModal.style.display = 'flex';
        const langSel = document.getElementById('appLanguageSelect');
        if (langSel) langSel.focus();
      }
    });

    on('appLanguageSelect', 'change', (e) => {
      const selectedLang = e.target.value;
      if (window.i18n) {
        window.i18n.setLanguage(selectedLang);
        Bridge.setSetting('app_language', selectedLang);
        showToast(window.i18n.t('toastLanguageChanged'));
      }
    });

    on('drawerSummaryBtn', 'click', () => {
      closeSideDrawer();
      openSummaryModal();
    });

    // Top Navigation Tabs (Screenshot 3)
    document.querySelectorAll('.top-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const screenId = btn.getAttribute('data-screen');
        if (screenId) switchScreen(screenId);
      });
    });

    // Summary Pill Button ("Özetini Görüntüle" - Screenshot 3)
    on(elements.summaryPillBtn, 'click', openSummaryModal);
    on('viewSummaryBtn', 'click', openSummaryModal);
    on(elements.closeSummaryBtn, 'click', () => {
      if (elements.summaryModal) elements.summaryModal.style.display = 'none';
    });
    on('closeSummaryBtn', 'click', () => {
      if (elements.summaryModal) elements.summaryModal.style.display = 'none';
    });
    on(elements.summaryCloseActionBtn, 'click', () => {
      if (elements.summaryModal) elements.summaryModal.style.display = 'none';
    });

    // Sort Button & Modal (Screenshot 4)
    on(elements.headerSortBtn, 'click', openSortModal);
    on('openSortModalBtn', 'click', openSortModal);
    on('viewOptionsBtn', 'click', openSortModal);
    on(elements.cancelSortBtn, 'click', () => {
      if (elements.sortModal) elements.sortModal.style.display = 'none';
    });
    on('cancelSortBtn', 'click', () => {
      if (elements.sortModal) elements.sortModal.style.display = 'none';
    });
    on(elements.applySortBtn, 'click', applySortFromModal);
    on('applySortBtn', 'click', applySortFromModal);

    // Dedicated Fullscreen Search Screen (Screenshot 6)
    on(elements.headerSearchBtn, 'click', openSearchScreen);
    on('openSearchBtn', 'click', openSearchScreen);
    on(elements.searchScreenBackBtn, 'click', closeSearchScreen);
    on('closeSearchScreenBtn', 'click', closeSearchScreen);

    if (elements.fullSearchInput) {
      let fullSearchDebounce;
      elements.fullSearchInput.addEventListener('input', (e) => {
        const q = e.target.value;
        const clearBtn = elements.clearFullSearchBtn || document.getElementById('clearDedicatedSearchBtn');
        if (clearBtn) {
          clearBtn.style.display = q.length > 0 ? 'flex' : 'none';
        }
        clearTimeout(fullSearchDebounce);
        fullSearchDebounce = setTimeout(() => {
          renderFullSearchResults(q);
        }, 180);
      });
    }

    on(elements.clearFullSearchBtn, 'click', () => {
      if (elements.fullSearchInput) {
        elements.fullSearchInput.value = '';
        if (elements.clearFullSearchBtn) elements.clearFullSearchBtn.style.display = 'none';
        renderFullSearchResults('');
      }
    });
    on('clearDedicatedSearchBtn', 'click', () => {
      const input = elements.fullSearchInput || document.getElementById('dedicatedSearchInput');
      if (input) {
        input.value = '';
        const btn = document.getElementById('clearDedicatedSearchBtn');
        if (btn) btn.style.display = 'none';
        renderFullSearchResults('');
      }
    });

    document.querySelectorAll('.search-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        document.querySelectorAll('.search-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        state.searchFilter = chip.getAttribute('data-filter') || 'all';
        const input = elements.fullSearchInput || document.getElementById('dedicatedSearchInput');
        if (input) {
          renderFullSearchResults(input.value);
        }
      });
    });

    // Action Pills: [ Karıştır Karıştır ] [ ▶ Oynat ] (Screenshot 3)
    on(elements.btnShuffleAll, 'click', () => {
      if (state.songs.length > 0) {
        Bridge.playAllShuffled(state.songs);
        showToast('Karışık çalma başlatıldı Karıştır');
      } else {
        showToast(tr('nothingToPlay'));
      }
    });

    on(elements.btnPlayAll, 'click', () => {
      if (state.songs.length > 0) {
        playQueueSong(state.songs, 0);
        showToast(tr('songsStarted'));
      } else {
        showToast(tr('nothingToPlay'));
      }
    });

    // Neon Theme Modal toggle
    on(elements.neonThemeBtn, 'click', () => {
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
    });
    on('neonThemeBtn', 'click', () => {
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
    });
    on(elements.closeNeonThemeBtn, 'click', () => {
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'none';
    });
    on('closeNeonThemeBtn', 'click', () => {
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'none';
    });

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
    on(elements.scanBtn, 'click', () => {
      if (elements.scanIcon) elements.scanIcon.style.animation = 'spin 0.8s linear infinite';
      Bridge.scanLibrary();
      showToast(tr('scanningMusic'));
    });
    on('btnRescanFromSettings', 'click', () => {
      Bridge.scanLibrary();
      showToast(tr('scanningMusic'));
      if (elements.settingsModal) elements.settingsModal.style.display = 'none';
    });

    // Grant Permission button
    on(elements.grantPermissionBtn, 'click', () => {
      Bridge.requestPermissions();
    });
    on('grantPermissionBtn', 'click', () => {
      Bridge.requestPermissions();
    });

    // Collection detail navigation
    on('artistDetailBackBtn', 'click', () => switchScreen('screenArtists'));
    on('albumDetailBackBtn', 'click', () => switchScreen('screenAlbums'));
    on('playlistDetailBackBtn', 'click', () => switchScreen('screenPlaylists'));
    on('artistDetailPlayBtn', 'click', () => {
      if (state.activeCollection && state.activeCollection.type === 'artist' && state.activeCollection.songs.length) {
        playQueueSong(state.activeCollection.songs, 0);
      }
    });
    on('albumDetailPlayBtn', 'click', () => {
      if (state.activeCollection && state.activeCollection.type === 'album' && state.activeCollection.songs.length) {
        playQueueSong(state.activeCollection.songs, 0);
      }
    });
    on('playlistDetailPlayBtn', 'click', () => {
      const songs = state.activeCollection?.songs || [];
      if (songs.length) playQueueSong(songs, 0);
      else showToast('Bu listede henüz şarkı yok');
    });

    // Search Input
    if (elements.searchInput) {
      let searchDebounce;
      elements.searchInput.addEventListener('input', (e) => {
        const q = e.target.value;
        state.searchQuery = q;
        if (q.trim()) state.folderFilter = null;
        if (elements.clearSearchBtn) elements.clearSearchBtn.style.display = q.length > 0 ? 'flex' : 'none';
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => {
          loadSongs();
        }, 200);
      });
    }

    on(elements.clearSearchBtn, 'click', () => {
      if (elements.searchInput) {
        elements.searchInput.value = '';
        state.searchQuery = '';
        state.folderFilter = null;
        if (elements.clearSearchBtn) elements.clearSearchBtn.style.display = 'none';
        loadSongs();
      }
    });

    // Sort Select
    if (elements.sortSelect) {
      elements.sortSelect.addEventListener('change', (e) => {
        state.sortOrder = e.target.value;
        loadSongs();
      });
    }

    // Bottom Navigation
    document.querySelectorAll('.nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const screenId = btn.getAttribute('data-screen');
        if (screenId) switchScreen(screenId);
      });
    });

    // Sub Tabs (Playlists / Library)
    document.querySelectorAll('.sub-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const parent = btn.parentElement;
        if (parent) {
          parent.querySelectorAll('.sub-tab-btn').forEach(b => b.classList.remove('active'));
        }
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
      if (parent) {
        parent.querySelectorAll('.sub-content').forEach(c => c.classList.remove('active'));
      }
      target.classList.add('active');
    }

    // SAF Picker
    on(elements.pickSafFolderBtn, 'click', () => {
      Bridge.pickFolder();
    });
    on('pickSafFolderBtn', 'click', () => {
      Bridge.pickFolder();
    });

    // Create Playlist
    on(elements.createPlaylistBtn, 'click', () => {
      if (!elements.newPlaylistInput) return;
      const name = elements.newPlaylistInput.value.trim();
      if (!name) return;
      Bridge.createPlaylist(name);
      elements.newPlaylistInput.value = '';
      loadPlaylists();
      showToast(`"${name}" listesi oluşturuldu`);
    });
    on('createPlaylistBtn', 'click', () => {
      const input = elements.newPlaylistInput || document.getElementById('newPlaylistInput');
      if (!input) return;
      const name = input.value.trim();
      if (!name) return;
      Bridge.createPlaylist(name);
      input.value = '';
      loadPlaylists();
      showToast(`"${name}" listesi oluşturuldu`);
    });

    // Clear History
    on(elements.clearHistoryBtn, 'click', () => {
      Bridge.clearHistory();
      loadHistory();
      showToast('Dinleme geçmişi temizlendi');
    });
    on('clearHistoryBtn', 'click', () => {
      Bridge.clearHistory();
      loadHistory();
      showToast('Dinleme geçmişi temizlendi');
    });

    // Mini Player Trigger (open Now Playing)
    on(elements.miniPlayerTrigger, 'click', () => setNowPlayingOpen(true));

    // Dedicated unified Play / Pause Handler ("Açma / Kapama")
    function handlePlayPauseToggle(e) {
      if (e) e.stopPropagation();
      if (state.isPlaying) {
        Bridge.pause();
      } else {
        if (!state.currentSong && state.songs.length > 0) {
          playQueueSong(state.songs, 0);
        } else {
          Bridge.resume();
        }
      }
    }

    on(elements.miniPlayBtn, 'click', handlePlayPauseToggle);
    on(elements.npPlayBtn, 'click', handlePlayPauseToggle);

    // Dedicated Next / Prev Track Handlers ("İleri Gitme / Geri Gelme")
    function handleNextTrack(e) {
      if (e) e.stopPropagation();
      Bridge.next();
    }

    function handlePrevTrack(e) {
      if (e) e.stopPropagation();
      Bridge.previous();
    }

    on(elements.miniNextBtn, 'click', handleNextTrack);
    on(elements.npNextBtn, 'click', handleNextTrack);
    on(elements.npPrevBtn, 'click', handlePrevTrack);

    on(elements.miniQueueBtn, 'click', (e) => {
      if (e) e.stopPropagation();
      openQueueModal();
    });

    // Now Playing Close
    on(elements.npCloseBtn, 'click', () => setNowPlayingOpen(false));

    // Synchronized (LRC) Lyrics Engine
    let lyricsRequestId = 0;

    async function loadLyricsForCurrentSong(force = false) {
      const song = state.currentSong;
      if (!song) return;
      if (!force && state.currentLyricsSongId === song.id && state.lyrics && state.lyrics.length > 0) return;

      state.currentLyricsSongId = song.id;
      state.currentLyricIndex = -1;
      const reqId = ++lyricsRequestId;
      const lyricsContainer = elements.lyricsContent || $('lyricsContent');
      if (!lyricsContainer) return;

      const cacheKey = `lrc_v2_${song.id}_${encodeURIComponent(song.uri || '')}`;
      try {
        const raw = localStorage.getItem(cacheKey);
        const cached = raw ? JSON.parse(raw) : null;
        if (cached?.lyrics?.length) {
          state.lyrics = cached.lyrics;
          renderLyrics(state.lyrics);
          syncLyricsScroll(state.position || 0, true);
          return;
        }
      } catch (e) {}

      lyricsContainer.innerHTML = `
        <div class="lyrics-status">
          <div class="lyrics-spinner"></div>
          <span>Şarkı sözleri aranıyor...</span>
          <span style="font-size:12px; color:var(--text-tertiary);">${escapeHtml(song.title)} - ${escapeHtml(song.artist)}</span>
        </div>`;

      const applyLyrics = (data) => {
        if (reqId !== lyricsRequestId || !state.currentSong || state.currentSong.id !== song.id) return true;
        if (!data || data.error) return false;
        if (data.instrumental) {
          lyricsContainer.innerHTML = `<div class="lyrics-status"><span class="lyrics-status-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="30" height="30" fill="currentColor"><path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6Z"/></svg></span><span style="font-weight:600;font-size:16px;">Bu parça enstrümantal</span><span style="font-size:12px;color:var(--text-tertiary);">Söz kaydı bulunmuyor</span></div>`;
          return true;
        }
        if (data.syncedLyrics?.trim()) {
          const parsed = parseLrc(data.syncedLyrics);
          if (parsed.length) {
            state.lyrics = parsed;
            try { localStorage.setItem(cacheKey, JSON.stringify({ lyrics: parsed })); } catch (e) {}
            renderLyrics(parsed);
            syncLyricsScroll(state.position || 0, true);
            return true;
          }
        }
        if (data.plainLyrics?.trim()) {
          const lines = data.plainLyrics.split(/\r?\n/).filter(l => l.trim());
          lyricsContainer.innerHTML = lines.map(l => `<p class="lyrics-line past" style="cursor:default;">${escapeHtml(l)}</p>`).join('');
          return true;
        }
        return false;
      };

      const showRetry = (message) => {
        if (reqId !== lyricsRequestId) return;
        lyricsContainer.innerHTML = `<div class="lyrics-status"><span class="lyrics-status-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm1 15h-2v-2h2Zm2.07-7.25-.9.92A3 3 0 0 0 13 13h-2c0-1 .37-1.88 1.07-2.59l1.24-1.27A1.64 1.64 0 0 0 13.9 8a1.9 1.9 0 0 0-3.8 0H8a4 4 0 0 1 8 0 3.14 3.14 0 0 1-.93 1.75Z"/></svg></span><span style="font-weight:600;font-size:15px;">${escapeHtml(message)}</span><span style="font-size:12px;color:var(--text-tertiary);max-width:260px;">${escapeHtml(song.title)} - ${escapeHtml(song.artist)}</span><button id="btnRetryLyrics" class="chip" style="margin-top:14px;cursor:pointer;background:var(--bg-surface-variant);border:1px solid var(--border-color);color:var(--text-primary);padding:6px 16px;border-radius:12px;">Tekrar Dene</button></div>`;
        document.getElementById('btnRetryLyrics')?.addEventListener('click', () => loadLyricsForCurrentSong(true));
      };

      // Android owns the LRCLIB request so native timeout, cancellation and
      // metadata validation stay in one place. Never issue a second browser request.
      if (!Bridge.isAvailable()) {
        showRetry('Lyrics yalnızca uygulama içinde kullanılabilir.');
        return;
      }
      Bridge.fetchLyricsAsync(song.title, song.artist, song.album, Math.round((song.duration || 0) / 1000), reqId);
    }

    function parseLrc(lrcText) {
      if (!lrcText || typeof lrcText !== 'string') return [];
      const lines = lrcText.split(/\r?\n/);
      const parsed = [];
      const timeRegex = /\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]/g;

      lines.forEach(rawLine => {
        const line = rawLine.trim();
        if (!line) return;

        const matches = [...line.matchAll(timeRegex)];
        if (matches.length > 0) {
          const text = line.replace(timeRegex, '').trim();
          matches.forEach(m => {
            const min = parseInt(m[1], 10);
            const sec = parseInt(m[2], 10);
            const msStr = m[3] || '0';
            const ms = msStr.length === 2 ? parseInt(msStr, 10) * 10 : parseInt(msStr.padEnd(3, '0').slice(0, 3), 10);
            const totalMs = (min * 60 + sec) * 1000 + ms;
            parsed.push({ timeMs: totalMs, text: text || '(müzik)' });
          });
        }
      });

      parsed.sort((a, b) => a.timeMs - b.timeMs);
      return parsed;
    }

    function renderLyrics(lines) {
      const container = elements.lyricsContent || $('lyricsContent');
      if (!container) return;
      container.innerHTML = '';

      const frag = document.createDocumentFragment();
      lines.forEach((item, idx) => {
        const el = document.createElement('div');
        el.className = 'lyrics-line';
        el.id = `lyric-line-${idx}`;
        el.setAttribute('data-time', item.timeMs);
        el.textContent = item.text;

        // Interactive tap-to-seek
        el.addEventListener('click', () => {
          Bridge.seek(item.timeMs);
          state.position = item.timeMs;
          syncLyricsScroll(item.timeMs, true);
        });

        frag.appendChild(el);
      });

      container.appendChild(frag);

      const panel = elements.npLyricsPanel || $('npLyricsPanel');
      if (panel && !panel._hasScrollListener) {
        panel._hasScrollListener = true;
        panel.addEventListener('scroll', () => {
          if (!panel._programmaticScroll) {
            state.userScrolledLyricsUntil = Date.now() + 2500;
          }
          panel._programmaticScroll = false;
        }, { passive: true });
      }
    }

    function syncLyricsScroll(currentMs, immediate = false) {
      const lines = state.lyrics;
      if (!lines || lines.length === 0) return;

      let activeIdx = -1;
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].timeMs <= currentMs + 200) {
          activeIdx = i;
        } else {
          break;
        }
      }

      if (activeIdx === state.currentLyricIndex && !immediate) return;
      state.currentLyricIndex = activeIdx;

      for (let i = 0; i < lines.length; i++) {
        const el = document.getElementById(`lyric-line-${i}`);
        if (!el) continue;
        if (i === activeIdx) {
          el.classList.add('active');
          el.classList.remove('past');
        } else if (i < activeIdx) {
          el.classList.remove('active');
          el.classList.add('past');
        } else {
          el.classList.remove('active', 'past');
        }
      }

      // Smooth auto-scroll to center active lyric line
      if (activeIdx >= 0 && Date.now() > (state.userScrolledLyricsUntil || 0)) {
        const activeEl = document.getElementById(`lyric-line-${activeIdx}`);
        const panel = elements.npLyricsPanel || $('npLyricsPanel');
        if (activeEl && panel) {
          panel._programmaticScroll = true;
          const panelHeight = panel.clientHeight;
          const lineTop = activeEl.offsetTop;
          const lineHeight = activeEl.clientHeight;
          const targetTop = lineTop - (panelHeight / 2) + (lineHeight / 2);

          panel.scrollTo({
            top: Math.max(0, targetTop),
            behavior: immediate ? 'auto' : 'smooth'
          });
        }
      }
    }

    // Segmented Buttons (Şarkı / Sözler)
    function switchNpSegment(segment) {
      const coverTab = $('tabNpCover') || elements.npSegSongBtn;
      const lyricsTab = $('tabNpLyrics') || elements.npSegLyricsBtn;
      const artContainer = $('npArtworkContainer') || elements.npArtworkContainer;
      const lyricsPanel = $('npLyricsPanel') || elements.npLyricsPanel;

      if (segment === 'cover') {
        state.isLyricsTabActive = false;
        if (coverTab) coverTab.classList.add('active');
        if (lyricsTab) lyricsTab.classList.remove('active');
        if (artContainer) artContainer.style.display = 'flex';
        if (lyricsPanel) lyricsPanel.style.display = 'none';
      } else {
        state.isLyricsTabActive = true;
        if (lyricsTab) lyricsTab.classList.add('active');
        if (coverTab) coverTab.classList.remove('active');
        if (artContainer) artContainer.style.display = 'none';
        if (lyricsPanel) lyricsPanel.style.display = 'block';
        loadLyricsForCurrentSong();
      }
    }

    on(elements.npSegSongBtn, 'click', () => switchNpSegment('cover'));
    on(elements.npSegLyricsBtn, 'click', () => switchNpSegment('lyrics'));

    // Header Options & Theme in Now Playing
    on(elements.npHeaderThemeBtn, 'click', () => {
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
    });

    on(elements.npHeaderOptionsBtn, 'click', () => {
      if (state.currentSong) openContextMenu(state.currentSong);
    });

    // Horizontal artwork gestures: swipe left/right for next/previous without
    // allowing vertical finger movement to turn the full player into a scroller.
    (() => {
      const artwork = $('npArtworkContainer') || elements.npArtworkContainer;
      if (!artwork) return;
      let startX = 0;
      let startY = 0;
      let tracking = false;
      artwork.addEventListener('pointerdown', (event) => {
        if (event.pointerType === 'mouse' && event.button !== 0) return;
        startX = event.clientX;
        startY = event.clientY;
        tracking = true;
        artwork.setPointerCapture?.(event.pointerId);
      }, { passive: true });
      artwork.addEventListener('pointerup', (event) => {
        if (!tracking) return;
        tracking = false;
        const dx = event.clientX - startX;
        const dy = event.clientY - startY;
        if (Math.abs(dx) < 56 || Math.abs(dx) < Math.abs(dy) * 1.25) return;
        if (dx < 0) Bridge.next();
        else Bridge.previous();
      }, { passive: true });
      artwork.addEventListener('pointercancel', () => { tracking = false; }, { passive: true });
    })();

    // Now Playing Quick Action Row
    on(elements.npFavQuickBtn, 'click', () => {
      if (!state.currentSong) return;
      const newFav = Bridge.toggleFavorite(state.currentSong.id);
      state.currentSong.isFavorite = newFav;
      updateNowPlayingFavoriteIcon(newFav);
      showToast(newFav ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
      loadFavorites();
    });

    on(elements.npAddPlaylistBtn, 'click', () => {
      if (state.currentSong) openAddToPlaylistPicker(state.currentSong);
    });

    on(elements.npEqQuickBtn, 'click', () => {
      if (elements.nowPlayingModal) setNowPlayingOpen(false);
      switchScreen('screenEqualizer');
    });

    on(elements.npTimerQuickBtn, 'click', () => {
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'flex';
    });

    on(elements.npQueueQuickBtn, 'click', openQueueModal);

    // Lyrics panel toggle
    on(elements.npLyricsBtn, 'click', () => {
      if (elements.npLyricsPanel) {
        const isVisible = elements.npLyricsPanel.style.display === 'flex';
        elements.npLyricsPanel.style.display = isVisible ? 'none' : 'flex';
      }
    });
    on(elements.closeLyricsBtn, 'click', () => {
      if (elements.npLyricsPanel) elements.npLyricsPanel.style.display = 'none';
    });

    // Settings Modal
    on(elements.settingsBtn, 'click', () => {
      if (elements.settingsModal) elements.settingsModal.style.display = 'flex';
    });
    on(elements.closeSettingsBtn, 'click', () => {
      if (elements.settingsModal) elements.settingsModal.style.display = 'none';
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

    // Settings Pick SAF Folder
    on(elements.btnPickSafFromSettings, 'click', () => {
      Bridge.pickSafFolder();
      if (elements.settingsModal) elements.settingsModal.style.display = 'none';
    });

    // Settings Rescan
    on(elements.btnRescanFromSettings, 'click', () => {
      Bridge.scanLibrary();
      showToast(tr('scanningMusic'));
      if (elements.settingsModal) elements.settingsModal.style.display = 'none';
    });

    // Settings Clear history
    on(elements.btnClearHistoryFromSettings, 'click', () => {
      Bridge.clearHistory();
      loadHistory();
      showToast('Çalma geçmişi sıfırlandı');
    });

    // Settings Open Theme Modal
    on(elements.openThemeFromSettings, 'click', () => {
      if (elements.settingsModal) elements.settingsModal.style.display = 'none';
      if (elements.neonThemeModal) elements.neonThemeModal.style.display = 'flex';
    });

    // Shuffle & Repeat in Now Playing
    on(elements.npShuffleBtn, 'click', () => {
      const nextShuffle = !state.isShuffle;
      state.isShuffle = nextShuffle;
      Bridge.toggleShuffle();
      if (elements.npShuffleBtn) elements.npShuffleBtn.classList.toggle('active', nextShuffle);
      showToast(nextShuffle ? tr('toastShuffleOn') : tr('toastShuffleOff'));
    });

    on(elements.npRepeatBtn, 'click', () => {
      let nextMode = ((state.repeatMode || 0) + 1) % 3;
      state.repeatMode = nextMode;
      Bridge.setRepeatMode(nextMode);
      updateRepeatModeUI(nextMode);
      const msgs = [tr('toastRepeatOff'), tr('toastRepeatAll'), tr('toastRepeatOne')];
      showToast(msgs[nextMode]);
    });

    // Seek Slider - Ultra Responsive, Smooth Scrubbing & Real-time Fill
    if (elements.npSeekSlider) {
      const slider = elements.npSeekSlider;

      const onSeekInput = (e) => {
        state.isSeeking = true;
        const dur = state.duration > 0 ? state.duration : (state.currentSong?.duration || 0);
        const val = parseFloat(slider.value);
        const ratio = Math.max(0, Math.min(1, val / 1000));
        const targetMs = Math.round(ratio * dur);
        const percent = (ratio * 100).toFixed(2);
        slider.style.background = `linear-gradient(to right, var(--primary) 0%, var(--primary) ${percent}%, rgba(255, 255, 255, 0.12) ${percent}%, rgba(255, 255, 255, 0.12) 100%)`;
        if (elements.npCurrentTime) elements.npCurrentTime.textContent = formatTime(targetMs);
        if (elements.miniProgressFill) elements.miniProgressFill.style.width = `${percent}%`;
      };

      const onSeekCommit = (e) => {
        const dur = state.duration > 0 ? state.duration : (state.currentSong?.duration || 0);
        const val = parseFloat(slider.value);
        const ratio = Math.max(0, Math.min(1, val / 1000));
        const targetMs = Math.round(ratio * dur);
        state.position = targetMs;
        updateProgressDisplay();
        Bridge.seek(targetMs);
        setTimeout(() => {
          state.isSeeking = false;
        }, 350);
      };

      slider.addEventListener('input', onSeekInput);
      slider.addEventListener('change', onSeekCommit);
      slider.addEventListener('pointerdown', () => { state.isSeeking = true; });
      slider.addEventListener('touchstart', () => { state.isSeeking = true; }, { passive: true });
    }

    // Tool buttons in Now Playing
    on(elements.npQueueBtn, 'click', openQueueModal);
    on(elements.closeQueueBtn, 'click', () => {
      if (elements.queueModal) elements.queueModal.style.display = 'none';
    });
    on('closeQueueBtn', 'click', () => {
      if (elements.queueModal) elements.queueModal.style.display = 'none';
    });
    on(elements.clearQueueBtn, 'click', () => {
      Bridge.clearQueue();
      if (elements.queueList) elements.queueList.innerHTML = '<div style="text-align:center; padding:30px; color:var(--text-tertiary);">Kuyruk temizlendi.</div>';
    });
    on('clearQueueBtn', 'click', () => {
      Bridge.clearQueue();
      const ql = elements.queueList || document.getElementById('queueList');
      if (ql) ql.innerHTML = '<div style="text-align:center; padding:30px; color:var(--text-tertiary);">Kuyruk temizlendi.</div>';
    });

    // Sleep Timer
    on(elements.npSleepTimerBtn, 'click', () => {
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'flex';
    });
    on(elements.closeSleepTimerBtn, 'click', () => {
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'none';
    });
    on('closeSleepTimerBtn', 'click', () => {
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'none';
    });
    on(elements.cancelSleepTimerBtn, 'click', () => {
      Bridge.cancelSleepTimer();
      if (elements.sleepTimerBadge) elements.sleepTimerBadge.style.display = 'none';
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'none';
      showToast('Zamanlayıcı iptal edildi');
    });
    on('cancelSleepTimerBtn', 'click', () => {
      Bridge.cancelSleepTimer();
      if (elements.sleepTimerBadge) elements.sleepTimerBadge.style.display = 'none';
      if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'none';
      showToast('Zamanlayıcı iptal edildi');
    });
    on(elements.cancelSleepBadgeBtn, 'click', () => {
      Bridge.cancelSleepTimer();
      if (elements.sleepTimerBadge) elements.sleepTimerBadge.style.display = 'none';
    });
    on('cancelSleepBadgeBtn', 'click', () => {
      Bridge.cancelSleepTimer();
      const badge = elements.sleepTimerBadge || document.getElementById('sleepTimerBadge');
      if (badge) badge.style.display = 'none';
    });

    document.querySelectorAll('.timer-opt-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const mins = parseInt(btn.getAttribute('data-min'));
        if (mins) {
          Bridge.setSleepTimer(mins);
          if (elements.sleepTimerModal) elements.sleepTimerModal.style.display = 'none';
          showToast(`Müzik ${mins} dakika sonra kapanacak`);
        }
      });
    });

    // Playback Speed
    on(elements.npSpeedBtn, 'click', () => {
      if (elements.speedModal) elements.speedModal.style.display = 'flex';
    });
    on(elements.closeSpeedBtn, 'click', () => {
      if (elements.speedModal) elements.speedModal.style.display = 'none';
    });
    on('closeSpeedBtn', 'click', () => {
      if (elements.speedModal) elements.speedModal.style.display = 'none';
    });

    document.querySelectorAll('.speed-opt-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const speed = parseFloat(btn.getAttribute('data-speed'));
        if (speed) {
          Bridge.setPlaybackSpeed(speed);
          state.playbackSpeed = speed;
          if (elements.npSpeedText) elements.npSpeedText.textContent = `${speed.toFixed(1)}x`;
          document.querySelectorAll('.speed-opt-btn').forEach(b => b.classList.remove('active'));
          btn.classList.add('active');
          if (elements.speedModal) elements.speedModal.style.display = 'none';
        }
      });
    });

    // Equalizer Shortcut
    on(elements.npEqShortcutBtn, 'click', () => {
      if (elements.nowPlayingModal) setNowPlayingOpen(false);
      switchScreen('screenEqualizer');
    });

    // Bass Boost Slider
    if (elements.bassBoostSlider) {
      elements.bassBoostSlider.addEventListener('input', (e) => {
        const val = parseInt(e.target.value);
        if (elements.bassBoostValueText) elements.bassBoostValueText.textContent = `${Math.round(val / 10)}%`;
        Bridge.setBassBoost(val);
      });
    }

    // Context Menu Buttons
    on(elements.ctxPlayNext, 'click', () => {
      if (state.selectedContextSong) Bridge.addToQueueNext(state.selectedContextSong);
      closeModals();
    });
    on(elements.ctxAddToQueue, 'click', () => {
      if (state.selectedContextSong) Bridge.addToQueueEnd(state.selectedContextSong);
      closeModals();
    });
    on(elements.ctxAddToPlaylist, 'click', () => {
      const song = state.selectedContextSong;
      closeModals();
      if (song) openAddToPlaylistPicker(song);
    });
    on(elements.ctxToggleFav, 'click', () => {
      if (state.selectedContextSong) {
        const newFav = Bridge.toggleFavorite(state.selectedContextSong.id);
        state.selectedContextSong.isFavorite = newFav;
        showToast(newFav ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
        loadFavorites();
        loadSongs();
      }
      closeModals();
    });
    on(elements.ctxShare, 'click', () => {
      if (state.selectedContextSong) Bridge.shareSong(state.selectedContextSong.id);
      closeModals();
    });
    on(elements.ctxDetails, 'click', () => {
      const song = state.selectedContextSong;
      closeModals();
      if (song) openSongDetails(song);
    });
    on(elements.ctxDelete, 'click', () => {
      if (state.selectedContextSong) {
        if (confirm(`"${state.selectedContextSong.title}" şarkısını silmek istediğinize emin misiniz?`)) {
          const deletedId = state.selectedContextSong.id;
          Bridge.deleteSong(deletedId);
          state.librarySongs = state.librarySongs.filter(song => song.id !== deletedId);
          state.songs = state.songs.filter(song => song.id !== deletedId);
          if (state.currentSong && state.currentSong.id === deletedId) state.currentSong = null;
          showToast('Şarkı silindi');
          loadSongs();
        }
      }
      closeModals();
    });
    on(elements.closeDetailsBtn, 'click', () => {
      if (elements.songDetailsModal) elements.songDetailsModal.style.display = 'none';
    });
    on(elements.closeAddToPlaylistBtn, 'click', () => {
      if (elements.addToPlaylistModal) elements.addToPlaylistModal.style.display = 'none';
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
    // If Side Drawer is open, close it
    if (elements.sideDrawerBackdrop && elements.sideDrawerBackdrop.style.display === 'flex') {
      elements.sideDrawerBackdrop.style.display = 'none';
      return true;
    }

    // If Search Screen is open, close it
    if (elements.searchScreen && elements.searchScreen.style.display === 'flex') {
      elements.searchScreen.style.display = 'none';
      return true;
    }

    // If Summary Modal is open, close it
    if (elements.summaryModal && elements.summaryModal.style.display === 'flex') {
      elements.summaryModal.style.display = 'none';
      return true;
    }

    // If Sort Modal is open, close it
    if (elements.sortModal && elements.sortModal.style.display === 'flex') {
      elements.sortModal.style.display = 'none';
      return true;
    }

    // If Now Playing is open, close it (or close lyrics panel first if active)
    if (elements.npLyricsPanel && elements.npLyricsPanel.style.display !== 'none' && elements.npLyricsPanel.style.display !== '') {
      elements.npLyricsPanel.style.display = 'none';
      if (elements.npArtworkContainer) elements.npArtworkContainer.style.display = 'flex';
      if (elements.npSegSongBtn) elements.npSegSongBtn.classList.add('active');
      if (elements.npSegLyricsBtn) elements.npSegLyricsBtn.classList.remove('active');
      return true;
    }
    if (elements.nowPlayingModal.classList.contains('active')) {
      setNowPlayingOpen(false);
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
      elements.addToPlaylistModal,
      elements.summaryModal,
      elements.sortModal
    ];

    for (const m of openModals) {
      if (m.style.display !== 'none' && m.style.display !== '') {
        m.style.display = 'none';
        return true;
      }
    }

    // Collection detail screens return to their parent library section
    if (state.activeScreen === 'screenArtistDetail') {
      switchScreen('screenArtists');
      return true;
    }
    if (state.activeScreen === 'screenAlbumDetail') {
      switchScreen('screenAlbums');
      return true;
    }
    if (state.activeScreen === 'screenPlaylistDetail') {
      switchScreen('screenPlaylists');
      return true;
    }

    // If not in Songs screen, navigate back to Songs
    if (state.activeScreen !== 'screenSongs') {
      switchScreen('screenSongs');
      return true;
    }

    return false; // Let Android finish or minimize
  };

  // --- Bridge Event Handlers from Native ---
  window.onLyricsResult = function (requestId, json) {
    try {
      if (requestId !== lyricsRequestId) return;
      const data = typeof json === 'string' ? JSON.parse(json) : json;
      const song = state.currentSong;
      if (!song) return;
      const lyricsContainer = elements.lyricsContent || $('lyricsContent');
      if (!lyricsContainer) return;
      if (data?.error) {
        lyricsContainer.innerHTML = `<div class="lyrics-status"><span class="lyrics-status-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm1 15h-2v-2h2Zm2.07-7.25-.9.92A3 3 0 0 0 13 13h-2c0-1 .37-1.88 1.07-2.59l1.24-1.27A1.64 1.64 0 0 0 13.9 8a1.9 1.9 0 0 0-3.8 0H8a4 4 0 0 1 8 0 3.14 3.14 0 0 1-.93 1.75Z"/></svg></span><span style="font-weight:600;font-size:15px;">Sözler alınamadı</span><span style="font-size:12px;color:var(--text-tertiary);max-width:260px;">${escapeHtml(data.error)}</span><button id="btnRetryLyrics" class="chip" style="margin-top:14px;cursor:pointer;background:var(--bg-surface-variant);border:1px solid var(--border-color);color:var(--text-primary);padding:6px 16px;border-radius:12px;">Tekrar Dene</button></div>`;
        document.getElementById('btnRetryLyrics')?.addEventListener('click', () => loadLyricsForCurrentSong(true));
        return;
      }
      if (data?.instrumental) {
        lyricsContainer.innerHTML = '<div class="lyrics-status"><span class="lyrics-status-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="30" height="30" fill="currentColor"><path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6Z"/></svg></span><span style="font-weight:600;font-size:16px;">Bu parça enstrümantal</span><span style="font-size:12px;color:var(--text-tertiary);">Söz kaydı bulunmuyor</span></div>';
        return;
      }
      if (data?.syncedLyrics?.trim()) {
        const parsed = parseLrc(data.syncedLyrics);
        if (parsed.length) {
          state.lyrics = parsed;
          try { localStorage.setItem(`lrc_v2_${song.id}_${encodeURIComponent(song.uri || '')}`, JSON.stringify({ lyrics: parsed })); } catch (e) {}
          renderLyrics(parsed);
          syncLyricsScroll(state.position || 0, true);
          return;
        }
      }
      if (data?.plainLyrics?.trim()) {
        const lines = data.plainLyrics.split(/\r?\n/).filter(l => l.trim());
        lyricsContainer.innerHTML = lines.map(l => `<p class="lyrics-line past" style="cursor:default;">${escapeHtml(l)}</p>`).join('');
        return;
      }
      const retry = document.getElementById('btnRetryLyrics');
      if (retry) retry.style.display = 'inline-flex';
    } catch (e) {
      console.warn('onLyricsResult parse error:', e);
    }
  };

  window.onPlaybackStateUpdated = function (data) {
    try {
      const stateObj = typeof data === 'string' ? JSON.parse(data) : data;
      lastNativePlaybackUpdateAt = Date.now();
      applyPlaybackState(stateObj);
    } catch (e) {
      console.error('onPlaybackStateUpdated parse error:', e);
    }
  };

  window.onScanCompleted = function (count) {
    showScanningState(false);
    if (count > 0) {
      showToast(`${count} şarkı kütüphanenize otomatik eklendi`);
    } else {
      showToast(tr('scanComplete'));
    }
    state.librarySongsLoaded = false;
    state.searchIndex.clear();
    state.folderFilter = null;
    loadLibraryData();
  };

  window.onScanFailed = function (err) {
    showScanningState(false);
    showToast('Tarama tamamlanamadı: ' + err);
  };

  window.onPermissionResult = function (granted) {
    checkPermissionsState();
    if (granted) {
      showToast(tr('permissionGranted'));
      showScanningState(true);
      Bridge.scanLibrary();
    } else {
      showScanningState(false);
      showToast(tr('permissionDenied'));
      loadLibraryData();
    }
  };

  window.onSafFolderScanned = function (count, uri) {
    showScanningState(false);
    showToast(`${count} ${tr('songsAddedFromFolder')}`);
    state.librarySongsLoaded = false;
    state.searchIndex.clear();
    loadLibraryData();
  };

  window.onSafFolderScanFailed = function (message) {
    showScanningState(false);
    const safeMessage = String(message || tr('folderScanFailed'));
    showToast(safeMessage);
  };

  window.onAppResumed = function () {
    checkPermissionsState();
    loadLibraryData();
    updatePlaybackTick();
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
