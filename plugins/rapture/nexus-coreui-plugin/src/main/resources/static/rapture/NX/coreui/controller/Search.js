/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Search controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Search', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n'
  ],

  masters: ['nx-coreui-search-result-list', 'nx-coreui-search-result-asset-list'],

  stores: [
    'Asset',
    'SearchFilter',
    'SearchCriteria',
    'SearchResult',
  ],
  models: [
    'SearchFilter'
  ],

  views: [
    'component.AssetContainer',
    'search.SearchFeature',
    'search.SearchResultAssetList',
    'search.SearchResultDetails',
    'search.SearchResultList',
    'search.TextSearchCriteria',
    'search.SaveSearchFilter'
  ],

  refs: [
    { ref: 'feature', selector: 'nx-coreui-searchfeature' },
    { ref: 'searchResult', selector: 'nx-coreui-search-result-list' },
    { ref: 'searchResultDetails', selector: 'nx-coreui-searchfeature #searchResultDetails' },
    { ref: 'searchResultAssets', selector: 'nx-coreui-search-result-asset-list' },
    { ref: 'assetContainer', selector: 'nx-coreui-searchfeature nx-coreui-component-assetcontainer' },
    { ref: 'quickSearch', selector: 'nx-header-panel #quicksearch' }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.getApplication().getIconController().addIcons({
      'search-default': {
        file: 'magnifier.png',
        variants: ['x16', 'x32']
      },
      'search-component': {
        file: 'box_front.png',
        variants: ['x16', 'x32']
      },
      'search-component-detail': {
        file: 'box_front_open.png',
        variants: ['x16', 'x32']
      },
      'search-folder': {
        file: 'folder_search.png',
        variants: ['x16', 'x32']
      },
      'search-saved': {
        file: 'magnifier.png',
        variants: ['x16', 'x32']
      }
    });

    me.getApplication().getFeaturesController().registerFeature({
      path: '/Search/Saved',
      mode: 'browse',
      group: true,
      iconName: 'search-folder',
      weight: 500,
      visible: function() {
        return NX.Permissions.check('nexus:repositories', 'read');
      }
    }, me);

    me.registerFilter([
      {
        id: 'keyword',
        name: 'Keyword',
        text: 'Keyword',
        description: 'Search for components by keyword',
        readOnly: true,
        criterias: [
          { id: 'keyword' }
        ]
      },
      {
        id: 'custom',
        name: 'Custom',
        text: NX.I18n.get('BROWSE_SEARCH_CUSTOM_TITLE'),
        description: NX.I18n.get('BROWSE_SEARCH_CUSTOM_SUBTITLE'),
        readOnly: true
      }
    ], me);

    me.getSearchFilterStore().each(function(model) {
      me.registerFeature(model, me);
    });

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.onRefresh
        }
      },
      component: {
        'nx-coreui-searchfeature': {
          afterrender: me.initCriterias
        },
        'nx-coreui-searchfeature menuitem[action=add]': {
          click: me.addCriteria
        },
        'nx-coreui-searchfeature component[searchCriteria=true]': {
          search: me.onSearchCriteriaChange,
          searchcleared: me.onSearchCriteriaChange,
          criteriaremoved: me.removeCriteria
        },
        'nx-coreui-searchfeature button[action=save]': {
          click: me.showSaveSearchFilterWindow
        },
        'nx-coreui-search-save button[action=add]': {
          click: me.saveSearchFilter
        },
        'nx-main #quicksearch': {
          afterrender: me.bindQuickSearch,
          search: me.onQuickSearch,
          searchcleared: me.onQuickSearch
        }
      }
    });
  },

  /**
   * @public
   * Register a set of criterias.
   * @param {Array/Object} criterias to be registered
   * @param {Ext.util.Observable} [owner] to be watched to automatically unregister the criterias if owner is destroyed
   */
  registerCriteria: function(criterias, owner) {
    var me = this,
        models;

    models = me.getSearchCriteriaStore().add(criterias);
    if (owner) {
      owner.on('destroy', function() {
        me.getSearchCriteriaStore().remove(models)
      }, me);
    }
  },

  /**
   * @public
   * Register a set of filters.
   * @param {Array/Object} filters to be registered
   * @param {Ext.util.Observable} [owner] to be watched to automatically unregister the criterias if owner is destroyed
   */
  registerFilter: function(filters, owner) {
    var me = this;

    Ext.each(Ext.Array.from(filters), function(filter) {
      me.registerFeature(me.getSearchFilterModel().create(filter), owner);
    });
  },

  /**
   * @private
   * Register feature for model.
   * @param {NX.coreui.model.SearchFilter} model to be registered
   * @param {Ext.util.Observable} [owner] to be watched to automatically unregister the criterias if owner is destroyed
   */
  registerFeature: function(model, owner) {
    var me = this;

    if (model.getId() === 'keyword') {
      me.getApplication().getFeaturesController().registerFeature({
        mode: 'browse',
        path: '/Search',
        text: NX.I18n.get('BROWSE_SEARCH_TITLE'),
        description: NX.I18n.get('BROWSE_SEARCH_SUBTITLE'),
        group: true,
        view: { xtype: 'nx-coreui-searchfeature', searchFilter: model, bookmarkEnding: '' },
        iconName: 'search-default',
        weight: 20,
        expanded: false,
        visible: function() {
          return NX.Permissions.check('nexus:repositories', 'read');
        }
      }, owner);
    }
    else {
      me.getApplication().getFeaturesController().registerFeature({
        mode: 'browse',
        path: '/Search/' + (model.get('readOnly') ? '' : 'Saved/') + model.get('name'),
        view: { xtype: 'nx-coreui-searchfeature', searchFilter: model, bookmarkEnding: '/' + model.getId() },
        iconName: 'search-default',
        text: model.get('text'),
        description: model.get('description'),
        authenticationRequired: false,
        visible: function() {
          return NX.Permissions.check('nexus:repositories', 'read');
        }
      }, owner);
    }
  },

  /**
   * @private
   * Avoid store load; manage load of search results by ourselves.
   */
  loadStore: function() {
    // do nothing for now
  },

  /**
   * @private
   * Show quick search when user has 'nexus:repositories:read' permission.
   */
  bindQuickSearch: function(quickSearch) {
    quickSearch.up('panel').mon(
        NX.Conditions.isPermitted('nexus:repositories', 'read'),
        {
          satisfied: quickSearch.show,
          unsatisfied: quickSearch.hide,
          scope: quickSearch
        }
    );
  },

  /**
   * @private
   * Initialize search criterias (filters) based on filter definition and bookmarked criterias.
   */
  initCriterias: function() {
    var me = this,
        searchPanel = me.getFeature(),
        searchFilter = searchPanel.searchFilter,
        searchCriteriaPanel = searchPanel.down('#criteria'),
        searchCriteriaStore = me.getSearchCriteriaStore(),
        addCriteriaMenu = [],
        bookmarkSegments = NX.Bookmarks.getBookmark().getSegments(),
        bookmarkValues = {},
        filterSegments,
        criterias = {}, criteriasPerGroup = {},
        searchCriteria, queryIndex, pair;

    // Extract the filter object from the URI
    if (bookmarkSegments && bookmarkSegments.length) {
      queryIndex = bookmarkSegments[0].indexOf('=');
      if (queryIndex != -1) {
        filterSegments = decodeURIComponent(bookmarkSegments[0].slice(queryIndex + 1)).split(' AND ');
        for (var i = 0; i < filterSegments.length; ++i) {
          pair = filterSegments[i].split('=');
          bookmarkValues[pair[0]] = pair[1];
        }
      }
    }

    searchCriteriaPanel.removeAll();
    me.getSearchResultStore().removeAll();
    me.getSearchResultStore().clearFilter(true);

    if (searchFilter && searchFilter.get('criterias')) {
      Ext.Array.each(Ext.Array.from(searchFilter.get('criterias')), function(criteria) {
        criterias[criteria['id']] = { value: criteria['value'], hidden: criteria['hidden'] };
      });
    }
    Ext.Object.each(bookmarkValues, function(key, value) {
      var existingCriteria = criterias[key];
      if (existingCriteria) {
        existingCriteria['value'] = value;
      }
      else {
        criterias[key] = { value: value, removable: true };
      }
    });

    Ext.Object.each(criterias, function(id, criteria) {
      var criteriaModel = searchCriteriaStore.getById(id);

      if (criteriaModel) {
        var cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-' + criteriaModel.getId());
        if (!cmpClass) {
          cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-text');
        }
        searchCriteria = searchCriteriaPanel.add(cmpClass.create(Ext.apply(Ext.clone(criteriaModel.get('config')), {
          criteriaId: criteriaModel.getId(),
          value: criteria['value'],
          hidden: criteria['hidden'],
          removable: criteria['removable']
        })));
        if (searchCriteria.value) {
          me.applyFilter(searchCriteria, false);
        }
      }
    });

    searchCriteriaStore.each(function(criteria) {
      var addTo = addCriteriaMenu,
          group = criteria.get('group');

      if (group) {
        if (!criteriasPerGroup[group]) {
          criteriasPerGroup[group] = [];
        }
        addTo = criteriasPerGroup[group];
      }
      addTo.push({
        text: criteria.get('config').fieldLabel,
        criteria: criteria,
        criteriaId: criteria.getId(),
        action: 'add',
        hidden: Ext.isDefined(criterias[criteria.getId()])
      });
    });
    Ext.Object.each(criteriasPerGroup, function(key, value) {
      addCriteriaMenu.push({
        text: key,
        menu: value
      });
    });

    searchCriteriaPanel.add({
      xtype: 'button',
      itemId: 'addButton',
      margin: '36px 0 0 0',
      text: NX.I18n.get('BROWSE_SEARCH_COMPONENTS_MORE_BUTTON'),
      glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
      menu: addCriteriaMenu
    });

    // HACK: fire a fake event to force paging toolbar to refresh
    me.getSearchResultStore().fireEvent('load', me);
    me.getSearchResultStore().filter();
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @private
   * Add a criteria.
   * @param menuitem selected criteria menu item
   */
  addCriteria: function(menuitem) {
    var me = this,
        searchPanel = me.getFeature(),
        searchCriteriaPanel = searchPanel.down('#criteria'),
        addButton = searchCriteriaPanel.down('#addButton'),
        criteria = menuitem.criteria,
        cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-' + criteria.getId()),
        cmp;

    menuitem.hide();
    if (!cmpClass) {
      cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-text');
    }
    searchCriteriaPanel.remove(addButton, false);
    cmp = cmpClass.create(
        Ext.apply(Ext.clone(criteria.get('config')), { criteriaId: criteria.getId(), value: undefined, removable: true })
    );
    searchCriteriaPanel.add(cmp);
    cmp.focus();
    searchCriteriaPanel.add(addButton);
  },

  /**
   * @private
   * Remove a criteria.
   * @param searchCriteria removed search criteria
   */
  removeCriteria: function(searchCriteria) {
    var me = this,
        searchPanel = me.getFeature(),
        searchCriteriaPanel = searchPanel.down('#criteria');

    searchCriteriaPanel.remove(searchCriteria);
    searchCriteriaPanel.down('menuitem[criteriaId=' + searchCriteria.criteriaId + ']').show();
    me.applyFilter({ criteriaId: searchCriteria.criteriaId }, true);
  },

  /**
   * @private
   * Start searching on criteria value changed.
   * @param searchCriteria changed criteria
   */
  onSearchCriteriaChange: function(searchCriteria) {
    var me = this;
    me.applyFilter(searchCriteria, true);
  },

  /**
   * @private
   * Search on refresh.
   */
  onRefresh: function() {
    var me = this;

    if (me.getFeature()) {
      me.getSearchResultStore().filter();
    }
  },

  /**
   * @private
   * Synchronize store filters with search criteria.
   * @param searchCriteria criteria to be synced
   * @param apply if filter should be applied on store ( = remote call)
   */
  applyFilter: function(searchCriteria, apply) {
    var me = this,
        store = me.getSearchResultStore(),
        filter = searchCriteria.filter;

    if (filter && Ext.isFunction(filter) && !(filter instanceof Ext.util.Filter)) {
      filter = searchCriteria.filter();
    }

    if (filter) {
      store.addFilter(Ext.apply(filter, { id: searchCriteria.criteriaId }), apply);
    }
    else {
      // TODO code bellow is a workaround stores not removing filters when remoteFilter = true
      store.removeFilter(searchCriteria.criteriaId);
      if (store.filters.removeAtKey(searchCriteria.criteriaId) && apply) {
        if (store.filters.length) {
          store.filter();
        }
        else {
          store.clearFilter();
        }
        store.fireEvent('filterchange', store, store.filters.items);
      }
    }

    if (apply) {
      me.onSearchResultSelection(null);
      me.bookmarkFilters();
    }
  },

  /**
   * @override
   * When a list managed by this controller is clicked, route the event to the proper handler
   */
  onSelection: function(list, model) {
    var me = this,
        modelType;

    // Figure out what kind of list we’re dealing with
    modelType = model.id.replace(/^.*?model\./, '').replace(/\-.*$/, '');

    if (modelType == "Component") {
      me.onSearchResultSelection(model);
    }
    else if (modelType == "Asset") {
      me.onSearchResultAssetSelection(model);
    }
  },

  /**
   * @private
   * Show details and load assets of selected component.
   * @param {NX.coreui.model.Component} model selected component
   */
  onSearchResultSelection: function(model) {
    var me = this,
        searchResultDetails = me.getSearchResultDetails(),
        assetsStore = me.getAssetStore(),
        info1 = {}, info2 = {};

    me.getAssetContainer().componentModel = model;
    me.onSearchResultAssetSelection(null);

    if (model) {
      info1[NX.I18n.get('BROWSE_SEARCH_ASSETS_REPOSITORY')] = model.get('repositoryName');
      info1[NX.I18n.get('BROWSE_SEARCH_ASSETS_FORMAT')] = model.get('format');
      info2[NX.I18n.get('BROWSE_SEARCH_ASSETS_GROUP')] = model.get('group');
      info2[NX.I18n.get('BROWSE_SEARCH_ASSETS_NAME')] = model.get('name');
      info2[NX.I18n.get('BROWSE_SEARCH_ASSETS_VERSION')] = model.get('version');

      searchResultDetails.down('#info1').showInfo(info1);
      searchResultDetails.down('#info2').showInfo(info2);
      assetsStore.clearFilter(true);
      assetsStore.addFilter([
        {
          property: 'repositoryName',
          value: model.get('repositoryName')
        },
        {
          property: 'componentId',
          value: model.getId()
        }
      ]);
    }
  },

  /**
   * @private
   * Show asset.
   * @param {NX.coreui.model.Asset} model selected asset
   */
  onSearchResultAssetSelection: function(model) {
    var me = this,
        assetContainer = me.getAssetContainer(),
        feature = me.getFeature();

    if (model) {
      me.getAssetContainer().assetModel = model;
      assetContainer.refreshInfo();
      assetContainer.expand();
      // Set the appropriate breadcrumb icon
      feature.setItemClass(2, assetContainer.iconCls);
      feature.setItemName(2, model.get('name'));
    }
    else {
      assetContainer.refreshInfo();
    }
  },

  /**
   * @private
   * Show "Save Search Filter" window.
   */
  showSaveSearchFilterWindow: function() {
    Ext.widget('nx-coreui-search-save');
  },

  /**
   * @private
   * Save a search filter.
   * @param {Ext.button.Button} button 'Add' button from "Save Search Filter"
   */
  saveSearchFilter: function(button) {
    var me = this,
        win = button.up('window'),
        values = button.up('form').getValues(),
        criterias = [],
        model;

    Ext.Array.each(Ext.ComponentQuery.query('nx-coreui-searchfeature component[searchCriteria=true]'), function(cmp) {
      criterias.push({
        id: cmp.criteriaId,
        value: cmp.getValue(),
        hidden: cmp.hidden
      });
    });

    model = me.getSearchFilterModel().create(Ext.apply(values, {
      id: values.name,
      criterias: criterias,
      readOnly: false
    }));

    me.getSearchFilterStore().add(model);

    me.getApplication().getFeaturesController().registerFeature({
      path: '/Search/' + (model.get('readOnly') ? '' : 'Saved/') + model.get('name'),
      mode: 'browse',
      view: { xtype: 'nx-coreui-searchfeature', searchFilter: model },
      iconName: 'search-saved',
      description: model.get('description'),
      authenticationRequired: false
    }, me);

    me.getController('Menu').refreshTree();
    NX.Bookmarks.navigateTo(NX.Bookmarks.fromToken('browse/search/saved/' + model.get('name')));

    win.close();
  },

  /**
   * @private
   * Bookmark search values.
   */
  bookmarkFilters: function() {
    var me = this,
        filterArray = [],
        firstSegment, segments;

    // Remove any pre-existing query string
    firstSegment = NX.Bookmarks.getBookmark().getSegment(0);
    if (firstSegment.indexOf('=') != -1) {
      firstSegment = firstSegment.slice(0, firstSegment.indexOf('='))
    }

    // Add each criteria to the filter object
    Ext.Array.each(Ext.ComponentQuery.query('nx-coreui-searchfeature component[searchCriteria=true]'), function(cmp) {
      if (cmp.getValue() && !cmp.isHidden()) {
        filterArray.push(cmp.criteriaId + '=' + cmp.getValue());
      }
    });

    // Stringify and url encode the filter object, then bookmark it
    segments = [firstSegment + "=" + encodeURIComponent(filterArray.join(' AND '))];
    NX.Bookmarks.bookmark(NX.Bookmarks.fromSegments(segments), me);
  },

  /**
   * @private
   * @param {NX.ext.SearchBox} quickSearch search box
   * @param {String} searchValue search value
   */
  onQuickSearch: function(quickSearch, searchValue) {
    var me = this,
        searchFeature = me.getFeature();

    if (!searchFeature || (searchFeature.searchFilter.getId() !== 'keyword')) {
      if (searchValue) {
        NX.Bookmarks.navigateTo(
            NX.Bookmarks.fromToken('browse/search=' + encodeURIComponent('keyword=' + searchValue)),
            me
        );
      }
    }
    else {
      searchFeature.down('#criteria component[criteriaId=keyword]').setValue(searchValue);
    }
  }

});
