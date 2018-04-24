define([ 'BaseContentEditor', 'events', 'repo', 'repo_controllers', './BaseTaskPropsView' ,'commentsComponent'],
	function( BaseContentEditor, events, repo, repo_controllers, BaseTaskPropsView, commentsComponent  ) {

	var BaseTaskEditor = BaseContentEditor.extend({

		initialize: function(config, configOverrides) {

			this._super(config, configOverrides);

			this.isTask = true;

			if (!configOverrides.previewMode) {

				this.bindEvents(
					{
						'deleteItem': {
							'type': 'register',
							'ctx': this,
							'unbind': 'dispose',
							'func': function f1024(id, dontShowDeleteNotification) {
								this.deleteItemById(id);
							}
						}
					});
				//initialize a comment component that allows the user to add comments in the task
				this.commentsComponent = new commentsComponent({data: this.record.data.comments,
                                            parentRecordId : this.record.id,
                                            $parent : $(".commentsArea")});
			}
		},

		deleteItemById: function (elementId) {

            var router = require('router');
            var activeMenuTab = router.activeScreen && router.activeScreen.components.menu && router.activeScreen.components.menu.menuInitFocusId;

            var index = repo.get(repo.get(elementId).parent).children.indexOf(elementId),
            parentEditorId = repo.remove(elementId);

            this.stage_view.$("[data-elementId='"+elementId+"']").remove();

            if (this.router && this.router.activeEditor) {
                this.router.activeEditor.startEditing();
            } else {
                if (router.activeScreen.constructor.type == 'TaskScreen' && router.activeEditor && router.activeEditor.startEditing) {
                    router.activeEditor.startEditing();

                }
                // //change the parent editor to be active after deletion of his son.
                // var parentEditor = repo_controllers.get(parentEditorId);
                // //editor that dont need to be edited(like answers editors) will only fire
                // parentEditor.startEditing ? parentEditor.startEditing() : events.fire('clickOnStage');
            }

            //re-set the tab menu that was active before the delete
            router.activeScreen.components.menu.setMenuTab(activeMenuTab);


            if (events.exists('contentEditorDeleted')) {
                events.fire('contentEditorDeleted', parentEditorId, index, elementId);
            }
            //after delete apply validation on parent
              require('validate').isEditorContentValid(parentEditorId);

        },

		components: {},
		handleComponents: function f55() {
			var _components = this.constructor.components;

			if (!_components) return false;
			else {
				_.each(_components, function f56(_component_item) {
					if (_component_item.condition && !_component_item.condition.call(this)) return false;

					if (!_component_item.name) {
						throw new TypeError("component name is not defined");
					} else {
						require([_component_item.type], function f57(comp) {
							_component_item.update = function f58(model, key, value) {
								model.set(key, value);

								require("repo").updateProperty(this.record.id, key, value);
							}.bind(this);

							_component_item.model = function f59(component) {
								var obj = {};

								_.each(component.component_model_fields, function f60(item) {
									obj[item] = this.record.data[item];
								}, this);

								return obj;
							}.bind(this, _component_item);

							!_component_item.onUpdateDataCallback && (_component_item.onUpdateDataCallback = function f61() {
								return false;
							})

							_component_item.controller = this;

							if (this.components[_component_item.name]) {
								this.components[_component_item.name].dispose();

								delete this.components[_component_item.name];
							}

							if (this.view && this.view.$(_component_item.parentContainer).length) {
								this.components[_component_item.name] = new comp(_component_item);
							}

						}.bind(this));
					}
				}, this);
			}
		},
		dispose: function(){
			//dispose comment component
			this.commentsComponent && this.commentsComponent.dispose();
			_.invoke(this.components, 'dispose');

			this._super();

			delete this.commentsComponent;
			delete this.components;
		},
		startEditing: function f63(elemId) {
			if (elemId) { // called from sequence screen on dblClick
				this.loadElement(elemId);
			}
			else {//called from Properties button
				this._super();
			}
		},

		startPropsEditing: function(){
			this._super();
			var parent = repo.getAncestorRecordByType(this.config.id, "sequence");
			this.showTaskSettingsButton = (typeof this.constructor.showTaskSettingsButton == "boolean" ? this.constructor.showTaskSettingsButton : true);

			if (!this.view) {
				this.view = new BaseTaskPropsView({controller:this});
			}
			else {
				this.view.initTemplate(); //default tab
				this.view.render();
				this.view.setInputMode();
				if (this.model) {
					this.model.off();
				}
			}

			this.handleComponents();

			this.registerEvents();

			if(this.showTaskSettingsButton) {
				this.showSettings();
			}

			this.view.toggleIntegrationSharedDdl((parent && parent.data && parent.data.type == "shared"));
		},

		registerEvents: function() {
			var changes = {
				difficulty: this.propagateChanges(this.record, 'difficulty', true)
			};

			this.model = this.screen.components.props.startEditing(this.record, changes);
			this.model.on('change:sharedIntegration',_.bind(this.changeSharedIntegration,this));
		},

		showSettings:function f65() {

			//get controller for every task child
			var child_controllers = _.map(this.record.children, function f66(childId) {
				return repo_controllers.get(childId);
			});

			//render each child props into task props
			_.each(child_controllers, function f67(controller, index, list) {
				controller && controller.startPropsEditing && controller.startPropsEditing(
					{'clearOnRender':false, 'contentSelector':'.tab-content #properties', 'appendToSelector':'.tab-content #settings'});
			});
		},

		//fired after change selection of integration with shared
		changeSharedIntegration: function(event, val){
			if (val.length > 0){
				repo.updateProperty(this.config.id, 'sharedIntegration', val);
			}
		},

		showComponent: function(componentName, show){
			//component exists, neet to display or hide it
			if(this.components && this.components[componentName]){
				if(show){
					this.components[componentName].show();
				}else{
					this.components[componentName].hide();
				}
			}else{
				//component dont exists, and need to display it- start the component
				if(show)
					this.handleComponents();
			}
		},

		endEditing: function(){
			if(this.model){
				this.model.unbind('change:sharedIntegration');
			}
			this._super();
		}

	}, {type: 'BaseTaskEditor'});

	return BaseTaskEditor;

});