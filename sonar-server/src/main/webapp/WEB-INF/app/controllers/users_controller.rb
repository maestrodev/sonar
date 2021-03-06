#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
### Users Controller from restful_authentication (http://agilewebdevelopment.com/plugins/restful_authentication)
class UsersController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required, :except => ['new', 'signup', 'autocomplete']
  skip_before_filter :check_authentication, :only => ['new', 'signup', 'autocomplete']

  def create
    return unless request.post?
    cookies.delete :auth_token

    user=prepare_user
    if user.save
      flash[:notice] = 'User is created.'
    end

    to_index(user.errors, nil);
  end

  def signup
    unless request.post? && Property.value('sonar.allowUsersToSignUp')=='true'
      return access_denied
    end

    cookies.delete :auth_token
    @user=prepare_user
    if @user.save
      flash[:notice] = 'Please log in now.'
      redirect_to home_url
    else
      render :action => 'new', :layout => 'nonav'
    end
  end

  def index
    @users = User.find(:all, :include => 'groups', :order => 'name')
    if params[:id]
      @user = User.find(params[:id])
    else
      @user = User.new
    end
  end

  def new
    render :action => 'new', :layout => 'nonav'
  end

  def edit
    redirect_to(:action => 'index', :id => params[:id])
  end

  def change_password
    @users = User.find(:all, :include => 'groups', :order => 'name')
    @user = User.find(params[:id])
    render :action => 'index', :id => params[:id]
  end

  def update_password
    user = User.find(params[:id])

    if params[:user][:password].blank?
      flash[:error] = 'Password required.'

    elsif user.update_attributes(:password => params[:user][:password], :password_confirmation => params[:user][:password_confirmation])
      flash[:notice] = 'Password was successfully updated.'
    end

    to_index(user.errors, nil);
  end

  def update
    user = User.find(params[:id])

    if user.login!=params[:user][:login]
      flash[:error] = 'Login can not be changed.'

    elsif user.update_attributes(params[:user])
      flash[:notice] = 'User was successfully updated.'
    end

    to_index(user.errors, nil);
  end

  def destroy
    @user = User.find(params[:id])

    if current_user.id==@user.id
      flash[:error] = 'Please log in with another user in order to delete yourself.'

    elsif @user.destroy
      flash[:notice] = 'User is deleted.'
    end

    to_index(@user.errors, nil);
  end

  def select_group
    @user = User.find(params[:id])
  end

  def set_groups
    @user = User.find(params[:id])

    if  @user.set_groups(params[:groups])
      flash[:notice] = 'User is updated.'
    end

    redirect_to(:action => 'index')
  end

  def to_index(errors, id)
    if !errors.empty?
      flash[:error] = errors.full_messages.join("<br/>\n")
    end

    redirect_to(:action => 'index', :id => id)
  end

  def toggle_edit_mode()
    current_user.toggle_edit_mode
    redirect_back_or_default(:controller => 'project')
  end


  def prepare_user
    user = User.new(params[:user])
    default_group_name=java_facade.getConfigurationValue('sonar.defaultGroup') || 'sonar-users';
    default_group=Group.find_by_name(default_group_name)
    user.groups<<default_group if default_group
    user
  end

  def autocomplete
    @users = User.find(:all, :conditions => ["UPPER(name) like ?", params[:user_name_start].clone.upcase+"%"])
    @char_count = params[:user_name_start].size
    render :partial => 'autocomplete'
  end

end
