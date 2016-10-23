'use strict';

function MainController($log, $state) {
  'ngInject';
  
  this.logger = function() {
    $log.log('heyyy');
  };

  this.mainCategoryLink = function(type){
    switch(type){
      case 1: $state.go('prevention'); break;
      case 2: $state.go('emergency'); break;
      case 3: $state.go('continuous'); break;
      default: $state.go('contact'); break; //<-- We should not be here, but eh.
    }
  };

}

export default MainController;
